package com.tenco.bank.handler.exception;

import org.springframework.http.HttpStatus;

public class UnAuthorizedException extends RuntimeException {

	private HttpStatus stauts;
	
	// throw new UnAuthorizedException ( , )
	public UnAuthorizedException(String message, HttpStatus status) {
		super(message);
		this.stauts = stauts;
	}
	
}
