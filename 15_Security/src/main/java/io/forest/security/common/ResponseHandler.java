package io.forest.security.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.forest.security.common.ResponseStatus.OK;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ResponseHandler<T> {

    T payload;

    ResponseStatus status;

    Throwable throwable;

    public <R> R ifOkOrElse(Function<T, R> okFunc, BiFunction<ResponseStatus, Throwable, R> errFunc) {
        return OK.equals(this.status)
                ? okFunc.apply(this.payload)
                : errFunc.apply(this.status, this.throwable);
    }

    public void ifOkOrElse(Consumer<T> okConsumer, BiConsumer<ResponseStatus, Throwable> errConsumer) {
        if (OK.equals(this.status)) {
            okConsumer.accept(this.payload);
        }
        else {
            errConsumer.accept(this.status, this.throwable);
        }
    }

    public static class Builder<T> {

        T payload;

        ResponseStatus status;

        Throwable throwable;

        public Builder<T> success(T payload) {
            this.payload = payload;
            return this;
        }

        public Builder<T> fail(ResponseStatus responseStatus, Throwable throwable) {
            this.status = responseStatus;
            this.throwable = throwable;
            return this;
        }

        public ResponseHandler<T> build() {
            return OK.equals(this.status)
                    ? new ResponseHandler<T>(this.payload, OK, null)
                    : new ResponseHandler<T>(null, this.status, this.throwable);
        }
    }
}
