package com.htphatz.identity_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_EXISTED(409, "User existed", HttpStatus.CONFLICT),
    ACCOUNT_BLOCKED(100000, "Account blocked", HttpStatus.UNAUTHORIZED),
    VOUCHER_EXISTED(409, "Voucher existed", HttpStatus.CONFLICT),
    ROLE_NOT_FOUND(404, "Role not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(404, "Category not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(404, "Product not found", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(404, "Order not found", HttpStatus.NOT_FOUND),
    ORDER_ITEM_NOT_FOUND(404, "Order item not found", HttpStatus.NOT_FOUND),
    VOUCHER_NOT_FOUND(404, "Voucher not found", HttpStatus.NOT_FOUND),
    PROVINCE_FOUND(404, "Province not found", HttpStatus.NOT_FOUND),
    DISTRICT_FOUND(404, "District not found", HttpStatus.NOT_FOUND),
    WARD_FOUND(404, "Ward not found", HttpStatus.NOT_FOUND),
    PASSWORD_INVALID(400, "Password invalid", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    QUANTITY_INVALID(400, "Quantity invalid", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(400, "Product is out of stock", HttpStatus.BAD_REQUEST),
    VOUCHER_EXPIRED(400, "Voucher is expired", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID(400, "Payment invalid", HttpStatus.BAD_REQUEST),
    ;

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(Integer code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
