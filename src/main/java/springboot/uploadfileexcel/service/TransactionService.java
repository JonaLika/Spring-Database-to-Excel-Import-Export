package springboot.uploadfileexcel.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;

public interface TransactionService {

    void importToDb(List<MultipartFile> multipartfiles);
    StreamingResponseBody exportToExcel(Long startDate, Long endDate, HttpServletResponse response);

}
