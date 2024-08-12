package com.tenco.bank.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tenco.bank.dto.DepositDTO;
import com.tenco.bank.dto.SaveDTO;
import com.tenco.bank.dto.TransferDTO;
import com.tenco.bank.dto.WithdrawalDTO;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.handler.exception.RedirectException;
import com.tenco.bank.repository.interfaces.AccountRepository;
import com.tenco.bank.repository.interfaces.HistoryRepository;
import com.tenco.bank.repository.model.Account;
import com.tenco.bank.repository.model.History;
import com.tenco.bank.repository.model.HistoryAccount;
import com.tenco.bank.utils.Define;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final HistoryRepository historyRepository;

	@Autowired
	public AccountService(AccountRepository accountRepository, HistoryRepository historyRepository) {
		this.accountRepository = accountRepository;
		this.historyRepository = historyRepository;
	}

	/*
	 * 계좌 생성 기능
	 * 
	 * @param dto
	 * 
	 * @param id
	 */

	// 트랜 잭션 처리
	@Transactional
	public void createAccount(SaveDTO dto, Integer principal) {

		int result = 0;

		try {

			// TODO principal
			result = accountRepository.insert(dto.toAccount(principal));
		} catch (DataAccessException e) {
			throw new DataDeliveryException("잘못된 요청입니다", HttpStatus.INTERNAL_SERVER_ERROR);

		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류 입니다.", HttpStatus.SERVICE_UNAVAILABLE);
		}

		if (result == 0) {
			throw new DataDeliveryException("정상 처리 되지 않았습니다", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	public List<Account> readAccountListByUserId(Integer userId) {
		List<Account> accountListEntity = null;

		try {
			accountListEntity = accountRepository.findByUserId(userId);

		} catch (DataAccessException e) {
			throw new DataDeliveryException("잘못된 처리 입니다", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
		}

		return accountListEntity;
	}

	// 한번에 모든 기능을 생각 힘듬
	// 1. 계좌 존재 여부를 확인 -- select
	// 2. 본인 계좌 여부를 확인 -- 객체 상태값에서 비교
	// 3. 계좌 비번 확인 -- 객체 상태값에서 일치 여부 확인
	// 4. 잔액 여부 확인 -- 객체 상태값에서 확인
	// 5. 출금 처리 -- update
	// 6. 거래 내역 등록 -- insert(history)
	// 7. 트랜잭션 처리

	@Transactional
	public void updateAccountWithdraw(WithdrawalDTO dto, Integer principalId) {
		// 1.
		Account accountEntitiy = accountRepository.findByNumber(dto.getWAccountNumber());
		if (accountEntitiy == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}
		// 2.
		accountEntitiy.checkOwner(principalId);

		// 3.
		accountEntitiy.checkPassword(dto.getWAccountPassword());

		// 4.

		accountEntitiy.checkBalance(dto.getAmount());

		// 5.
		// accountEntity 객체의 잔액을 변경하고 업데이트 처리해야 한다.
		accountEntitiy.withdraw(dto.getAmount());

		accountRepository.updateById(accountEntitiy);

		// 6 - 거래 내역 등록
		/*
		 * insert into history_tb(amount,, w_balance, d_balance, w_account_id,
		 * d_account_id) values(#{amount}, #{w_balance}, #{d_balance}, #{w_account_id},
		 * #{d_account_id} )
		 * 
		 */
		History history = new History();
		history.setAmount(dto.getAmount());
		history.setWBalance(accountEntitiy.getBalance());
		history.setDBalance(null);
		history.setWAccountId(accountEntitiy.getId());
		history.setDAccountId(null);

		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	// 입금 기능 만들기
	// 1. 계좌 존재 여부 확인 select
	// 2. 본인 계좌 여부를 확인 -- 객체 상태값에서 비교
	// 4. 입금 처리 -- update
	// 5. 거래 내역 등록 -- insert(history)
	// 6. 트랜잭션 처리

	@Transactional
	public void updateAccountDeposit(DepositDTO dto, Integer principalId) {

		Account accountEntitiy = accountRepository.findByNumber(dto.getDAccountNumber());

		if (accountEntitiy == null) {
			throw new DataDeliveryException(Define.D_BALANCE_VALUE, HttpStatus.BAD_REQUEST);
		}

		accountEntitiy.checkOwner(principalId);

		accountEntitiy.deposit(dto.getAmount());

		accountRepository.updateById(accountEntitiy);

		// 6 - 거래 내역 등록
		/*
		 * insert into history_tb(amount,, w_balance, d_balance, w_account_id,
		 * d_account_id) values(#{amount}, #{w_balance}, #{d_balance}, #{w_account_id},
		 * #{d_account_id} )
		 * 
		 */
		History history = new History();
		history.setAmount(dto.getAmount());
		history.setWBalance(null);
		history.setDBalance(accountEntitiy.getBalance());
		history.setWAccountId(null);
		history.setDAccountId(accountEntitiy.getId());

		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// 이체 기능 만들기
	// 1. 출금 계좌 존재 여부 확인 -- select
	// 2. 입금 계좌 존재 여부 확인 -- select
	// 3. 출금 계좌 본인 소유 확인 -- 객체 상태값과 세션 아이디 비교
	// 4. 출금 계좌 비밀 번호 확인 -- 객체 상태값과 dto 비밀번호 비교
	// 5. 출금 계좌 잔액 여부 확인 -- 객체 상태값 확인, dto와 비교
	// 6. 입금 계좌 객체 상태값 변경 처리 (거래금액 증가)
	// 7. 입금 계좌 -- update 처리
	// 8. 출금 계좌 객체 상태값 변경 처리 ( 잔액 - 거래금액)
	// 9. 출금 계좌 -- update 처리
	// 10. 거래 내역 등록 처리 -- insert
	// 11. 트랜잭션 처리 - ACID 처리

	@Transactional
	public void updateAccountTransfer(TransferDTO dto, Integer principalId) {

		Account depositEntitiy = accountRepository.findByNumber(dto.getDAccountNumber());
		Account withdrawEntitiy = accountRepository.findByNumber(dto.getWAccountNumber());
		// 1
		if (withdrawEntitiy == null) {
			// 출금 계좌 번호가 DB 에 존재하지 않음 !
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}

		// 2
		if (depositEntitiy == null) {
			// 입금 계좌 번호가 DB 에 존재하지 않음 !
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}

		// 3. 출금 계좌 본인 소유 확인 -- 객체 상태값과 세션 아이디 비교
		withdrawEntitiy.checkOwner(principalId);

		// 4. 출금 계좌 비밀 번호 확인 -- 객체 상태값과 dto 비밀번호 비교
		withdrawEntitiy.checkPassword(dto.getPassword());

		// 5. 출금 계좌 잔액 여부 확인 -- 객체 상태값 확인, dto와 비교
		withdrawEntitiy.checkBalance(dto.getAmount());

		// 6. 입금 계좌 객체 상태값 변경 처리 (거래금액 증가)
		depositEntitiy.deposit(dto.getAmount());

		// 7. 입금 계좌 -- update 처리
		depositEntitiy.deposit(dto.getAmount());
		accountRepository.updateById(depositEntitiy);

		// 8. 출금 계좌 객체 상태값 변경 처리 ( 잔액 - 거래금액)
		// 9. 출금 계좌 -- update 처리
		withdrawEntitiy.withdraw(dto.getAmount());
		accountRepository.updateById(withdrawEntitiy);

		// 10. 거래 내역 등록 처리 -- insert

		History history = new History();
		history.setAmount(dto.getAmount());
		history.setWBalance(withdrawEntitiy.getBalance());
		history.setDBalance(depositEntitiy.getBalance());
		history.setWAccountId(withdrawEntitiy.getId());
		history.setDAccountId(depositEntitiy.getId());

		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// 11. 트랜잭션 처리 - ACID 처리

	}

	/**
	 * 단일 계좌 조회 기능
	 * @param accountId (px)
	 * @return
	 */
	public Account readAccountByID(Integer accountId) {
		Account accountEntity = accountRepository.findByAccountId(accountId);
		if (accountEntity == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return accountEntity;

	}

	/**
	 * 단일 계좌 거래 내역 조회
	 * @param type = [all, deposit, withdrawal]
	 * @param accountId (pk)
	 * @return 전체, 입금, 출금 거래내역(3가지 타입) 반환
	 */
	// @Transactional
	public List<HistoryAccount> readHistoryByAccountId(String type, Integer accountId){
		List<HistoryAccount> list = new ArrayList<>();
		list = historyRepository.findByAccountIdAndTypeOfHistory(type, accountId);
		return list;
		
	}
}
