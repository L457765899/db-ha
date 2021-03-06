package com.sxb.lin.trx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.alibaba.druid.pool.xa.DruidXADataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.sxb.lin.db.ha.mybatis.interceptor.QueryInterceptor;
import com.sxb.lin.db.ha.mybatis.transaction.HASpringManagedTransactionFactory;
import com.sxb.lin.db.ha.slave.SlaveQuerier;
import com.sxb.lin.db.ha.slave.SlaveRocketMqQuerier;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

@MapperScan("com.sxb.lin.trx.db.dao")
@Configuration
public class HaConfig extends WebMvcConfigurerAdapter{
	
	@Bean
	public JedisCluster redisCluster(){
		Set<HostAndPort> set = new HashSet<HostAndPort>();
        HostAndPort hap0 = new HostAndPort("192.168.0.250",6000);
        HostAndPort hap1 = new HostAndPort("192.168.0.250",6001);
        HostAndPort hap2 = new HostAndPort("192.168.0.250",6002);
        HostAndPort hap3 = new HostAndPort("192.168.0.250",6003);
        HostAndPort hap4 = new HostAndPort("192.168.0.250",6004);
        HostAndPort hap5 = new HostAndPort("192.168.0.250",6005);
        set.add(hap0);
        set.add(hap1);
        set.add(hap2);
        set.add(hap3);
        set.add(hap4);
        set.add(hap5);
        JedisCluster redisCluster = new JedisCluster(set);
        return redisCluster;
	}
	
	@Bean(initMethod="start",destroyMethod="shutdown")
	public DefaultMQProducer defaultMQProducer() {
		DefaultMQProducer producer = new DefaultMQProducer("TEST_DB_HA");
		producer.setNamesrvAddr("192.168.0.252:9876");
		return producer;
	}
	
	@Autowired
	@Bean(initMethod="init",destroyMethod="destroy")
	public Interceptor queryInterceptor(JedisCluster jedisCluster, DefaultMQProducer producer){
		SlaveQuerier slaveQuerier = new SlaveRocketMqQuerier(jedisCluster, producer);
		QueryInterceptor queryInterceptor = new QueryInterceptor();
		queryInterceptor.setSlaveQuerier(slaveQuerier);
		return queryInterceptor;
	}
	
	@Bean(initMethod = "init",destroyMethod = "close")
	@ConfigurationProperties("spring.datasource.druid")
	public DataSource dataSource(){
		return new DruidXADataSource();
	}
	
	@Bean
	@Autowired
	public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource,Interceptor interceptor) throws IOException{
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();  
        bean.setDataSource(dataSource);
        bean.setMapperLocations(resolver.getResources("classpath:com/sxb/lin/trx/db/mapping/*.xml"));
        bean.setPlugins(new Interceptor[]{interceptor});
        bean.setTransactionFactory(new HASpringManagedTransactionFactory());
        return bean;
	}

	@Bean
    @Autowired
    public DefaultPointcutAdvisor defaultPointcutAdvisor(PlatformTransactionManager transactionManager){
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* com.sxb.lin.trx.service.impl..*(..))");
        
        Properties attributes = new Properties();
        attributes.setProperty("add*", "PROPAGATION_REQUIRED,-Exception");
        /*attributes.setProperty("save*", "PROPAGATION_REQUIRED,-Exception");*/
        attributes.setProperty("update*", "PROPAGATION_REQUIRED,-Exception");
        attributes.setProperty("edit*", "PROPAGATION_REQUIRED,-Exception");
        attributes.setProperty("delete*", "PROPAGATION_REQUIRED,-Exception");
        attributes.setProperty("remove*", "PROPAGATION_REQUIRED,-Exception");
        
        attributes.setProperty("get*", "PROPAGATION_SUPPORTS,readOnly");
        attributes.setProperty("find*", "PROPAGATION_REQUIRED,readOnly");
        attributes.setProperty("load*", "PROPAGATION_REQUIRED,readOnly");
        attributes.setProperty("search*", "PROPAGATION_REQUIRED,readOnly");
        attributes.setProperty("select*", "PROPAGATION_REQUIRED,readOnly");
        attributes.setProperty("check*", "PROPAGATION_REQUIRED,readOnly");
        TransactionInterceptor advice = new TransactionInterceptor(transactionManager, attributes);
        
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setPointcut(pointcut);
        advisor.setAdvice(advice);
        advisor.setOrder(Ordered.LOWEST_PRECEDENCE);
        
        return advisor;
    }
	
	@Bean
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setDefaultEncoding("UTF-8");
        multipartResolver.setMaxUploadSize(512 * 1024 * 1024);
        multipartResolver.setMaxInMemorySize(5 * 1024 * 1024);
        return multipartResolver;
    }
	
	@Bean
    public ServletRegistrationBean statViewServlet(){
        ServletRegistrationBean registration = new ServletRegistrationBean(new StatViewServlet(),"/druid/*");
        //登录查看信息的账号密码.
        registration.addInitParameter("loginUsername","admin");
        registration.addInitParameter("loginPassword","Sxb889961");
        //是否能够重置数据.
        registration.addInitParameter("resetEnable","false");
        return registration;
    }
	
	@Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        
        List<MediaType> stringMediaTypes = new ArrayList<>();
        stringMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        
        StringHttpMessageConverter StringConverter = new StringHttpMessageConverter();
        StringConverter.setSupportedMediaTypes(stringMediaTypes);
        StringConverter.setWriteAcceptCharset(false);
        converters.add(StringConverter);
        
    }
}
