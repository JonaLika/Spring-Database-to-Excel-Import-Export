package springboot.uploadfileexcel.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springboot.uploadfileexcel.service.TransactionService;

import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping(path = "/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping(path = "/import-to-db")
    public void importTransactionsFromExcelToDb(@RequestPart(required = true) List<MultipartFile> files) {

        transactionService.importToDb(files);

    }

    @GetMapping(path = "/export-to-excel")
    public ResponseEntity<StreamingResponseBody> downloadTransactions(
            @RequestParam(name = "startDate", required = true) Long startDate,
            @RequestParam(name = "endDate", required = true) Long endDate, HttpServletResponse response) {

        return ResponseEntity.ok(transactionService.exportToExcel(startDate, endDate, response));

    }
}
