package zerobase.weather.config;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)  // 모든 예외에 대해 동작하도록 한다.
    public Exception handleAllException(){
        System.out.println("Error from GlobalExceptionHandler ");
        return new Exception();
    }
}
