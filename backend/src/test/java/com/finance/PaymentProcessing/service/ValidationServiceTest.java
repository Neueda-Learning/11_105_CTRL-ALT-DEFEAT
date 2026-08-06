package com.finance.PaymentProcessing.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.BankAccount;
import com.finance.PaymentProcessing.model.Beneficiary;
import com.finance.PaymentProcessing.model.CardType;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private BankAccountRepository accountRepository;

    private ValidationService service;

    @BeforeEach
    void setUp() {
        service = new ValidationService(beneficiaryRepository, accountRepository);
    }

    private BankAccount account(String number, boolean active) {
        BankAccount a = new BankAccount();
        a.setAccountNumber(number);
        a.setActive(active);
        return a;
    }

    private Beneficiary beneficiary(String number) {
        Beneficiary b = new Beneficiary();
        b.setAccountNumber(number);
        return b;
    }

    // ---- validateAmount ----

    @Test
    void validateAmount_null_throws() {
        assertThrows(BadRequestException.class, () -> service.validateAmount(null));
    }

    @Test
    void validateAmount_zero_throws() {
        assertThrows(BadRequestException.class, () -> service.validateAmount(BigDecimal.ZERO));
    }

    @Test
    void validateAmount_negative_throws() {
        assertThrows(BadRequestException.class, () -> service.validateAmount(new BigDecimal("-1")));
    }

    @Test
    void validateAmount_positive_ok() {
        assertDoesNotThrow(() -> service.validateAmount(BigDecimal.TEN));
    }

    // ---- validateCurrency ----

    @Test
    void validateCurrency_null_throws() {
        assertThrows(BadRequestException.class, () -> service.validateCurrency(null));
    }

    @Test
    void validateCurrency_unsupported_throws() {
        assertThrows(BadRequestException.class, () -> service.validateCurrency("JPY"));
    }

    @Test
    void validateCurrency_supported_ok() {
        assertDoesNotThrow(() -> service.validateCurrency("inr"));
    }

    // ---- validateBeneficiary ----

    @Test
    void validateBeneficiary_missing_throws() {
        when(beneficiaryRepository.existsById("missing")).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.validateBeneficiary("missing"));
    }

    @Test
    void validateBeneficiary_exists_ok() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        assertDoesNotThrow(() -> service.validateBeneficiary("100000001"));
    }

    // ---- validateSourceAccount ----

    @Test
    void validateSourceAccount_accountMissing_throwsNotFound() {
        when(accountRepository.findById("acc")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.validateSourceAccount("acc", "ben"));
    }

    @Test
    void validateSourceAccount_beneficiaryMissing_throwsNotFound() {
        when(accountRepository.findById("acc")).thenReturn(Optional.of(account("ACC111", true)));
        when(beneficiaryRepository.findById("ben")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.validateSourceAccount("acc", "ben"));
    }

    @Test
    void validateSourceAccount_inactiveAccount_throwsBadRequest() {
        when(accountRepository.findById("acc")).thenReturn(Optional.of(account("ACC111", false)));
        when(beneficiaryRepository.findById("ben")).thenReturn(Optional.of(beneficiary("ACC222")));
        assertThrows(BadRequestException.class, () -> service.validateSourceAccount("acc", "ben"));
    }

    @Test
    void validateSourceAccount_sameAccountNumber_throwsBadRequest() {
        when(accountRepository.findById("acc")).thenReturn(Optional.of(account("ACC111", true)));
        when(beneficiaryRepository.findById("ben")).thenReturn(Optional.of(beneficiary("ACC111")));
        assertThrows(BadRequestException.class, () -> service.validateSourceAccount("acc", "ben"));
    }

    @Test
    void validateSourceAccount_validDistinctActiveAccounts_ok() {
        when(accountRepository.findById("acc")).thenReturn(Optional.of(account("ACC111", true)));
        when(beneficiaryRepository.findById("ben")).thenReturn(Optional.of(beneficiary("ACC222")));
        assertDoesNotThrow(() -> service.validateSourceAccount("acc", "ben"));
    }

    // ---- validatePaymentDetails ----

    @Test
    void validatePaymentDetails_nullType_ok() {
        assertDoesNotThrow(() -> service.validatePaymentDetails(null, null));
    }

    @Test
    void validatePaymentDetails_billPaymentWithoutInvoice_throws() {
        assertThrows(BadRequestException.class,
                () -> service.validatePaymentDetails(PaymentType.BILL_PAYMENT, " "));
    }

    @Test
    void validatePaymentDetails_billPaymentWithInvoice_ok() {
        assertDoesNotThrow(() -> service.validatePaymentDetails(PaymentType.BILL_PAYMENT, "INV-1"));
    }

    @Test
    void validatePaymentDetails_beneficiaryTransferWithInvoice_throws() {
        assertThrows(BadRequestException.class,
                () -> service.validatePaymentDetails(PaymentType.BENEFICIARY_TRANSFER, "INV-1"));
    }

    @Test
    void validatePaymentDetails_beneficiaryTransferWithoutInvoice_ok() {
        assertDoesNotThrow(() -> service.validatePaymentDetails(PaymentType.BENEFICIARY_TRANSFER, null));
    }

    // ---- validateMethodSpecificDetails ----

    @Test
    void validateMethodSpecificDetails_nullMethod_throws() {
        assertThrows(BadRequestException.class,
                () -> service.validateMethodSpecificDetails(null, null, null, null, null, null, null, null, null));
    }

    @Test
    void validateMethodSpecificDetails_netBanking_missingBeneficiary_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.NET_BANKING, null, null, null, null, null, null, null, null));
    }

    @Test
    void validateMethodSpecificDetails_netBanking_withCardFields_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.NET_BANKING, "ben", CardType.CREDIT_CARD, null, null, null, null, null, null));
    }

    @Test
    void validateMethodSpecificDetails_netBanking_valid_ok() {
        assertDoesNotThrow(() -> service.validateMethodSpecificDetails(
                PaymentMethod.NET_BANKING, "ben", null, null, null, null, null, null, null));
    }

    @Test
    void validateMethodSpecificDetails_upi_withBeneficiary_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, "ben", null, null, null, null, null, null, "user@upi"));
    }

    @Test
    void validateMethodSpecificDetails_upi_withCardFields_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, null, CardType.CREDIT_CARD, null, null, null, null, null, "user@upi"));
    }

    @Test
    void validateMethodSpecificDetails_upi_invalidUpiId_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, null, null, null, null, null, null, null, "not-a-upi"));
    }

    @Test
    void validateMethodSpecificDetails_upi_blankUpiId_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, null, null, null, null, null, null, null, "  "));
    }

    @Test
    void validateMethodSpecificDetails_upi_valid_ok() {
        assertDoesNotThrow(() -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, null, null, null, null, null, null, null, "user@upi"));
    }

    @Test
    void validateMethodSpecificDetails_card_withBeneficiary_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, "ben", CardType.CREDIT_CARD, "Name", "4111111111111111", "12", "2099", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_missingCardType_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, null, "Name", "4111111111111111", "12", "2099", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_missingHolderName_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, " ", "4111111111111111", "12", "2099", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_invalidLuhn_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, "Name", "4111111111111112", "12", "2099", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_tooShortNumber_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, "Name", "1234", "12", "2099", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_nonNumericExpiry_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, "Name", "4111111111111111", "ab", "cd", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_monthOutOfRange_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, "Name", "4111111111111111", "13", "2099", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_expired_throws() {
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, "Name", "4111111111111111", "01", "2000", "123", null));
    }

    @Test
    void validateMethodSpecificDetails_card_invalidCvv_throws() {
        String futureYear = String.valueOf(YearMonth.now().plusYears(2).getYear());
        assertThrows(BadRequestException.class, () -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, "Name", "4111111111111111", "12", futureYear, "12345", null));
    }

    @Test
    void validateMethodSpecificDetails_card_valid_ok() {
        String futureYear = String.valueOf(YearMonth.now().plusYears(2).getYear());
        assertDoesNotThrow(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null, CardType.CREDIT_CARD, "Name", "4111111111111111", "12", futureYear, "123", null));
    }

    @Test
    void normalizeCardNumber_stripsNonDigits() {
        assertEquals("4111111111111111", service.normalizeCardNumber("4111-1111-1111-1111"));
    }

    // ---- validateStatusTransition ----

    @Test
    void validateStatusTransition_createdToValidated_ok() {
        assertDoesNotThrow(() -> service.validateStatusTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED));
    }

    @Test
    void validateStatusTransition_createdToFailed_ok() {
        assertDoesNotThrow(() -> service.validateStatusTransition(PaymentStatus.CREATED, PaymentStatus.FAILED));
    }

    @Test
    void validateStatusTransition_createdToCompleted_throws() {
        assertThrows(BadRequestException.class,
                () -> service.validateStatusTransition(PaymentStatus.CREATED, PaymentStatus.COMPLETED));
    }

    @Test
    void validateStatusTransition_validatedToSent_ok() {
        assertDoesNotThrow(() -> service.validateStatusTransition(PaymentStatus.VALIDATED, PaymentStatus.SENT));
    }

    @Test
    void validateStatusTransition_validatedToFailed_ok() {
        assertDoesNotThrow(() -> service.validateStatusTransition(PaymentStatus.VALIDATED, PaymentStatus.FAILED));
    }

    @Test
    void validateStatusTransition_sentToCompleted_ok() {
        assertDoesNotThrow(() -> service.validateStatusTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED));
    }

    @Test
    void validateStatusTransition_sentToFailed_ok() {
        assertDoesNotThrow(() -> service.validateStatusTransition(PaymentStatus.SENT, PaymentStatus.FAILED));
    }

    @Test
    void validateStatusTransition_completedToAnything_throws() {
        assertThrows(BadRequestException.class,
                () -> service.validateStatusTransition(PaymentStatus.COMPLETED, PaymentStatus.FAILED));
    }

    @Test
    void validateStatusTransition_failedToAnything_throws() {
        assertThrows(BadRequestException.class,
                () -> service.validateStatusTransition(PaymentStatus.FAILED, PaymentStatus.SENT));
    }
}
