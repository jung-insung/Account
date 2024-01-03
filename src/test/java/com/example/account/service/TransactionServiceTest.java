package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.*;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

class TransactionServiceTest {
    @ExtendWith(MockitoExtension.class)
    class AccountServiceTest {
        @Mock
        private TransactionRepository transactionRepository;
        @Mock
        private AccountRepository accountRepository;

        @Mock
        private AccountUserRepository accountUserRepository;

        @InjectMocks
        private TransactionService transactionService;

        @Test
        void successUseBalance() {
            //given
            AccountUser user = AccountUser.builder()
                    .id(15L)
                    .name("Pobi").build();
            given(accountUserRepository.findById(anyLong()))
                    .willReturn(Optional.of(user));
            Account account = Account.builder()
                    .accountUser(user)
                    .accountStatus(IN_USE)
                    .balance(10000L)
                    .accountNumber("100000012").build();
            given(accountRepository.findByAccountNumber(anyString()))
                    .willReturn(Optional.of(account));
            given(transactionRepository.save(any()))
                    .willReturn(Transaction.builder()
                            .account(account)
                            .transactionType(USE)
                            .transactionResultType(S)
                            .transactionId("transactionId")
                            .transactedAt(LocalDateTime.now())
                            .amount(1000L)
                            .balanceSnapshot(9000L)
                            .build());
            //when
            TransactionDto transactionDto = transactionService.useBalance(1L, "1000000000", 1000L);

            //then
            assertEquals(9000L, transactionDto.getTransactionResultType());
            assertEquals(USE, transactionDto.getTransactionType());
            assertEquals(9000L, transactionDto.getBalanceSnapshot());
            assertEquals(1000L, transactionDto.getAmount());
        }

        @Test
        @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
        void deleteAccount_AccountNotFound() {
            //given
            given(accountRepository.findByAccountNumber(anyString()))
                    .willReturn(Optional.empty());

            //when
            AccountException exception = assertThrows(AccountException.class,
                    () -> transactionService.useBalance(1L, "1000000000", 1000L));

            //then
            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
        void deleteAccountFailed_userUnMatch() {
            //given
            AccountUser pobi = AccountUser.builder()
                    .id(12L)
                    .name("Pobi").build();
            AccountUser harry = AccountUser.builder()
                    .id(13L)
                    .name("Harry").build();
            given(accountUserRepository.findById(anyLong()))
                    .willReturn(Optional.of(pobi));
            given(accountRepository.findByAccountNumber(anyString()))
                    .willReturn(Optional.of(Account.builder()
                            .accountUser(harry)
                            .balance(0L)
                            .accountNumber("100000012").build()));
            //when
            AccountException exception = assertThrows(AccountException.class,
                    () -> transactionService.useBalance(1L, "1234567890",1000L));

            //then
            assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
        }
    }
}