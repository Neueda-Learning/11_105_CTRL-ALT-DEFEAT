package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.CardType;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("INR", "USD", "EUR", "GBP");
    private static final Pattern UPI_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{2,256}@[a-zA-Z]{2,64}$");
    private final BeneficiaryRepository beneficiaryRepository;
    private final BankAccountRepository accountRepository;

    public ValidationService(BeneficiaryRepository beneficiaryRepository, BankAccountRepository accountRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0)
            throw new BadRequestException("Amount must be greater than zero");
    }

    public void validateCurrency(String currency) {
        if (currency == null || !SUPPORTED_CURRENCIES.contains(currency.toUpperCase()))
            throw new BadRequestException("Unsupported currency. Use INR, USD, EUR, or GBP");
    }

    public void validateBeneficiary(String beneficiaryId) {
        if (!beneficiaryRepository.existsById(beneficiaryId))
            throw new NotFoundException("Beneficiary not found: " + beneficiaryId);
    }

    public void validateSourceAccount(String sourceAccountId, String beneficiaryId) {
        var account = accountRepository.findById(sourceAccountId)
            .orElseThrow(() -> new NotFoundException("INVALID_ACCOUNT", "Source account not found: " + sourceAccountId));
        var beneficiary = beneficiaryRepository.findById(beneficiaryId)
            .orElseThrow(() -> new NotFoundException("INVALID_ACCOUNT", "Beneficiary not found: " + beneficiaryId));
        if (!account.isActive() || account.getAccountNumber().equals(beneficiary.getAccountNumber()))
            throw new BadRequestException("INVALID_ACCOUNT", "Source and destination accounts must be different active accounts");
    }

    public void validatePaymentDetails(PaymentType paymentType, String invoiceId) {
        if (paymentType == null) {
            return;
        }
        if (paymentType == PaymentType.BILL_PAYMENT && (invoiceId == null || invoiceId.isBlank())) {
            throw new BadRequestException("invoiceId is required for a bill payment");
        }
        if (paymentType == PaymentType.BENEFICIARY_TRANSFER && invoiceId != null && !invoiceId.isBlank()) {
            throw new BadRequestException("invoiceId must not be supplied for a beneficiary transfer");
        }
    }

    public void validateMethodSpecificDetails(PaymentMethod paymentMethod,
            String beneficiaryId,
            CardType cardType,
            String cardHolderName,
            String cardNumber,
            String expiryMonth,
            String expiryYear,
            String cvv,
            String upiId) {
        if (paymentMethod == null) {
            throw new BadRequestException("paymentMethod is required");
        }
        if (paymentMethod == PaymentMethod.NET_BANKING) {
            validateNetBankingDetails(beneficiaryId, cardType, cardHolderName, cardNumber, expiryMonth, expiryYear, cvv, upiId);
            return;
        }
        if (paymentMethod == PaymentMethod.UPI) {
            validateUpiDetails(beneficiaryId, cardType, cardHolderName, cardNumber, expiryMonth, expiryYear, cvv, upiId);
            return;
        }
        validateCardDetails(beneficiaryId, cardType, cardHolderName, cardNumber, expiryMonth, expiryYear, cvv);
    }

    private void validateNetBankingDetails(String beneficiaryId,
            CardType cardType,
            String cardHolderName,
            String cardNumber,
            String expiryMonth,
            String expiryYear,
            String cvv,
            String upiId) {
        if (beneficiaryId == null) {
            throw new BadRequestException("VALIDATION_FAILED", "beneficiaryId is required for net banking");
        }
        if (cardType != null || hasText(cardHolderName) || hasText(cardNumber)
                || hasText(expiryMonth) || hasText(expiryYear) || hasText(cvv) || hasText(upiId)) {
            throw new BadRequestException("VALIDATION_FAILED", "card fields are not allowed for net banking");
        }
    }

    private void validateUpiDetails(String beneficiaryId,
            CardType cardType,
            String cardHolderName,
            String cardNumber,
            String expiryMonth,
            String expiryYear,
            String cvv,
            String upiId) {
        if (beneficiaryId != null) {
            throw new BadRequestException("VALIDATION_FAILED", "beneficiaryId must not be supplied for UPI payment");
        }
        if (cardType != null || hasText(cardHolderName) || hasText(cardNumber)
                || hasText(expiryMonth) || hasText(expiryYear) || hasText(cvv)) {
            throw new BadRequestException("VALIDATION_FAILED", "card fields are not allowed for UPI payment");
        }
        if (!hasText(upiId) || !UPI_ID_PATTERN.matcher(upiId.trim()).matches()) {
            throw new BadRequestException("INVALID_UPI", "UPI ID is invalid");
        }
    }

    private void validateCardDetails(String beneficiaryId,
            CardType cardType,
            String cardHolderName,
            String cardNumber,
            String expiryMonth,
            String expiryYear,
            String cvv) {
        if (beneficiaryId != null) {
            throw new BadRequestException("VALIDATION_FAILED", "beneficiaryId must not be supplied for card payment");
        }
        if (cardType == null) {
            throw new BadRequestException("INVALID_CARD", "Card type is required");
        }
        if (!hasText(cardHolderName)) {
            throw new BadRequestException("INVALID_CARD", "Cardholder name is required");
        }

        String digits = onlyDigits(cardNumber);
        if (digits.length() < 13 || digits.length() > 19 || !isLuhnValid(digits)) {
            throw new BadRequestException("INVALID_CARD", "Card number is invalid");
        }

        int month;
        int year;
        try {
            month = Integer.parseInt(expiryMonth);
            year = Integer.parseInt(expiryYear);
        } catch (Exception ex) {
            throw new BadRequestException("INVALID_CARD", "Card expiry is invalid");
        }

        if (month < 1 || month > 12) {
            throw new BadRequestException("INVALID_CARD", "Expiry month must be between 1 and 12");
        }

        YearMonth expiry = YearMonth.of(year, month);
        if (expiry.isBefore(YearMonth.now())) {
            throw new BadRequestException("INVALID_CARD", "Card is expired");
        }

        if (!hasText(cvv) || !cvv.trim().matches("\\d{3,4}")) {
            throw new BadRequestException("INVALID_CARD", "CVV must be 3 or 4 digits");
        }
    }

    public String normalizeCardNumber(String cardNumber) {
        return onlyDigits(cardNumber);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static boolean isLuhnValid(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (doubleDigit) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    public void validateStatusTransition(PaymentStatus oldStatus, PaymentStatus newStatus) {
        boolean valid = switch (oldStatus) {
            case CREATED   -> newStatus == PaymentStatus.VALIDATED || newStatus == PaymentStatus.FAILED;
            case VALIDATED -> newStatus == PaymentStatus.SENT      || newStatus == PaymentStatus.FAILED;
            case SENT      -> newStatus == PaymentStatus.COMPLETED || newStatus == PaymentStatus.FAILED;
            default        -> false;
        };
        if (!valid)
            throw new BadRequestException("Invalid status transition from " + oldStatus + " to " + newStatus);
    }
}
