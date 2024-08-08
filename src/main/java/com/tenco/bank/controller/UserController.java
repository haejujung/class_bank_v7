package com.tenco.bank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tenco.bank.dto.SignInDTO;
import com.tenco.bank.dto.SignUpDTO;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.repository.model.User;
import com.tenco.bank.service.UserService;
import com.tenco.bank.utils.Define;

import jakarta.servlet.http.HttpSession;

@Controller 
@RequestMapping("/user") 
public class UserController {
	
	private UserService userService;
	private final HttpSession session;
	
	@Autowired
	public UserController(UserService userService, HttpSession session) {
		this.userService = userService;
		this.session = session;
	}
	
	// 주소설계 -> http://localhost:8080/user/sign-up
	/*
	 * 회원 가입 페이지 요청
	 * 주소 설계 : http://localhost:8080/user/sign-up
	 * @return signUp.jsp
	 */
	@GetMapping("/sign-up")
	public String signUpPage() {
		return "user/signUp";
		
	}
	/*
	 * 회원 가입 로직 처리 요청
	 * 주소 설계 : http://localhost:8080/user/sign-up
	 * @param dto
	 * @return 
	 */
	
	@PostMapping("/sign-up")
	public String signUpProc(SignUpDTO dto) {
		
		if(dto.getUsername() == null || dto.getUsername().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_USERNAME, HttpStatus.BAD_REQUEST);
		}
		if(dto.getPassword() == null || dto.getPassword().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD , HttpStatus.BAD_REQUEST);
		}
		if(dto.getFullname() == null || dto.getFullname().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_ACCOUNT_NUMBER , HttpStatus.BAD_REQUEST);
		}
		userService.createUser(dto);
		
		// TODO - 추후 수정
		return "redirect:/user/sign-in";
		
	}
	/*
	 * 로그인 화면 요청
	 * 주소설계 : http://localhost:8080/user/sign-in
	 */
	
	@GetMapping("/sign-in")
	public String signInPage() {
		// 인증검사 x
		// 유효성검사 x
		return "user/signIn";
		
	}
	
	/*
	 * 로그인 요청 처리
	 * 주소설계 : http://localhost:8080/user/sign-in
	 * @return
	 */
	
	
	
	@PostMapping("/sign-in")
	public String signProc(SignInDTO dto) {
		
		// 1. 인증 검사 x
		// 2. 유효성 검사
		if(dto.getUsername() == null || dto.getUsername().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_USERNAME, HttpStatus.BAD_REQUEST);
		}
		
		if(dto.getUsername() == null || dto.getPassword().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}
		
		// 서비스 호출
		User principal =  userService.readUser(dto);
		
		// 세션 메모리에 등록 처리
		session.setAttribute(Define.PRINCIPAL, principal);
		
		return "redirect:/account/list";
	}
	
	@GetMapping("/logout")
	public String logout() {
		session.invalidate(); // 로그아웃 됨
		return "redirect:/user/sign-in";
		
	}
	
	
}
