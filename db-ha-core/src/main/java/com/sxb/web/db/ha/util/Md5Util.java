package com.sxb.web.db.ha.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Md5Util {
	
	private static Logger logger = LoggerFactory.getLogger(Md5Util.class);

	public static String md5Hex(String text){
		String hex = null;
		try {
			MessageDigest md = MessageDigest.getInstance("md5");
			byte[] b = md.digest(text.getBytes());
			hex = fromBytesToHex(b);
		} catch (NoSuchAlgorithmException e) {
			hex = UUID.randomUUID().toString().toUpperCase();
			logger.error(e.getMessage(), e);
		}
		return hex;
	}
	
	public static String fromBytesToHex(byte[] resultBytes) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < resultBytes.length; i++) {
			String hexString = Integer.toHexString(0xFF & resultBytes[i]);
			if (hexString.length() == 1) {
				builder.append("0").append(hexString);
			} else {
				builder.append(hexString);
			}
		}
		return builder.toString();
	}
}
