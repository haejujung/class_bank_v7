package com.tenco.bank.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
	 * 회원 가입 페이지 요청 주소 설계 : http://localhost:8080/user/sign-up
	 * 
	 * @return signUp.jsp
	 */
	@GetMapping("/sign-up")
	public String signUpPage() {
		return "user/signUp";

	}
	/*
	 * 회원 가입 로직 처리 요청 주소 설계 : http://localhost:8080/user/sign-up
	 * 
	 * @param dto
	 * 
	 * @return
	 */

	@PostMapping("/sign-up")
	public String signUpProc(SignUpDTO dto) {

		if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_USERNAME, HttpStatus.BAD_REQUEST);
		}
		if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}
		if (dto.getFullname() == null || dto.getFullname().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_ACCOUNT_NUMBER, HttpStatus.BAD_REQUEST);
		}
		userService.createUser(dto);

		// TODO - 추후 수정
		return "redirect:/user/sign-in";

	}
	/*
	 * 로그인 화면 요청 주소설계 : http://localhost:8080/user/sign-in
	 */

	@GetMapping("/sign-in")
	public String signInPage() {
		// 인증검사 x
		// 유효성검사 x
		return "user/signIn";

	}

	/*
	 * 로그인 요청 처리 주소설계 : http://localhost:8080/user/sign-in
	 * 
	 * @return
	 */

	@PostMapping("/sign-in")
	public String signProc(SignInDTO dto) {

		// 1. 인증 검사 x
		// 2. 유효성 검사
		if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_USERNAME, HttpStatus.BAD_REQUEST);
		}

		if (dto.getUsername() == null || dto.getPassword().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}

		// 서비스 호출
		User principal = userService.readUser(dto);

		// 세션 메모리에 등록 처리
		session.setAttribute(Define.PRINCIPAL, principal);

		return "redirect:/account/list";
	}

	@GetMapping("/logout")
	public String logout() {
		session.invalidate(); // 로그아웃 됨
		return "redirect:/user/sign-in";

	}

	@ResponseBody
	@GetMapping("/kakao")
	public String signinkakao(@RequestParam(name = "code") String code) {
		
		URI uri = UriComponentsBuilder
				.fromUriString("https://kauth.kakao.com/oauth/token")
				.build()
				.toUri();
		
		RestTemplate restTemplate1 = new RestTemplate();
		
		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
		
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", "d3071232250f394d8cbfc6349a1581ee");
		params.add("redirect_uri", "http://localhost:8080/user/kakao");
		params.add("code", code);
		
		HttpEntity<MultiValueMap<String, String>> requestEntity
		= new HttpEntity<>(params,headers);
		
		ResponseEntity<String> response = restTemplate1
				.exchange(uri, HttpMethod.POST, requestEntity, String.class);	
		
		System.out.println(response.toString());

		String message = response.toString();
		String[] arraymessage = message.split("\"");
		String tocken = arraymessage[3];
		System.out.println(tocken);
		
		URI uri2 = UriComponentsBuilder
				.fromUriString("https://kapi.kakao.com/v2/user/me")
				.build()
				.toUri();
		
		RestTemplate restTemplate2 = new RestTemplate();

		HttpHeaders headers2 = new HttpHeaders();
		
		headers2.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
		headers2.add("Authorization", " Bearer "+tocken);
		
		HttpEntity<MultiValueMap<String, String>> requestEntity2
		= new HttpEntity<>(null,headers2);
		
		ResponseEntity<String> response2 = restTemplate2
				.exchange(uri2, HttpMethod.POST, requestEntity2, String.class);	
		
		return response2.getBody();
	}

}
