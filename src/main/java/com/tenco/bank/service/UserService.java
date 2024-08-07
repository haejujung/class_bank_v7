package com.tenco.bank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tenco.bank.dto.SignInDTO;
import com.tenco.bank.dto.SignUpDTO;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.handler.exception.RedirectException;
import com.tenco.bank.repository.interfaces.UserRepository;
import com.tenco.bank.repository.model.User;

@Service // ioc 대상 (싱글톤으로 관리
public class UserService {

	// DI - 의존 주입
	private UserRepository userRepository;
	
	@Autowired
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
		
	}

//	@Autowired 어노테이션으로 대체 가능하다.
//	public UserService(UserRepository userRepository) {
//		this.userRepository = userRepository;
//	}

	// 회원 가입 처리

	/*
	 * 회원 등록 서비스 기능 트랜잭션 처리
	 * 
	 * @param dto
	 */
	@Transactional // 트랜잭션 처리는 반드시 습관화
	public void createUser(SignUpDTO dto) {

		int result = 0;

		try {
			result = userRepository.insert(dto.toUser());

		} catch (DataAccessException e) {
			throw new DataDeliveryException("잘못된 처리입니다, 중복 이름을 사용 할 수 없습니다. ", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
		}

		if (result != 1) {
			throw new DataDeliveryException("회원가입 실패 ", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
		
		public User readUser(SignInDTO dto) {	
			// 유효성 검사는 Controller 에서 먼저 하자.
			User userEntitiy = null; // 지역 변수 선언
			try {
				userEntitiy = userRepository.findByUsernameAndPassword(dto.getUsername(), dto.getPassword());	

			} catch (DataAccessException e) {
				throw new DataDeliveryException("잘못된 처리입니다. ", HttpStatus.INTERNAL_SERVER_ERROR);
			} catch(Exception e) {
				throw new RedirectException("알수 없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
			}
			
			if(userEntitiy == null) {
				throw new DataDeliveryException("아이디 혹은 비밀번호가 틀렸습니다.", HttpStatus.BAD_REQUEST);
			}
			
			return userEntitiy;
		}

	}

