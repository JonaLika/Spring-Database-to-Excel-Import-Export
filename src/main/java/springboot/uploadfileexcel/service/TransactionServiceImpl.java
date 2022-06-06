package springboot.uploadfileexcel.service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import springboot.uploadfileexcel.entity.Transaction;
import springboot.uploadfileexcel.exception.InvalidRequestException;
import springboot.uploadfileexcel.repository.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;


    @Override
    public void importToDb(List<MultipartFile> multipartfiles) {
        if (!multipartfiles.isEmpty()) {
            List<Transaction> transactions = new ArrayList<>();
            multipartfiles.forEach(multipartfile -> {
                try {
                    XSSFWorkbook workBook = new XSSFWorkbook(multipartfile.getInputStream());

                    XSSFSheet sheet = workBook.getSheetAt(0);
                    // looping through each row
                    for (int rowIndex = 0; rowIndex < getNumberOfNonEmptyCells(sheet, 0) - 1; rowIndex++) {
                        // current row
                        XSSFRow row = sheet.getRow(rowIndex);
                        // skip the first row because it is a header row
                        if (rowIndex == 0) {
                            continue;
                        }
                        Long senderId = Long.parseLong(getValue(row.getCell(0)).toString());
                        Long receiverId = Long.parseLong(getValue(row.getCell(1)).toString());
                        Long initiatorId = Long.parseLong(getValue(row.getCell(2)).toString());
                        String bankCode = String.valueOf(row.getCell(3));
                        Integer serviceCode = Integer.parseInt(getValue(row.getCell(4)).toString());
                        double transactionAmount = Double.parseDouble(row.getCell(5).toString());
                        double feeAmount = Double.parseDouble(row.getCell(6).toString());

                        Transaction transaction = Transaction.builder().senderId(senderId).receiverId(receiverId)
                                .initiatorId(initiatorId).bankCode(bankCode).serviceCode(serviceCode)
                                .trxnAmount(transactionAmount).feeAmount(feeAmount).build();
                        transactions.add(transaction);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            if (!transactions.isEmpty()) {
                // save to database
                transactionRepository.saveAll(transactions);
            }
        }
    }

    private Object getValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case ERROR:
                return cell.getErrorCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            case _NONE:
                return null;
            default:
                break;
        }
        return null;
    }

    public static int getNumberOfNonEmptyCells(XSSFSheet sheet, int columnIndex) {
        int numOfNonEmptyCells = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                XSSFCell cell = row.getCell(columnIndex);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    numOfNonEmptyCells++;
                }
            }
        }
        return numOfNonEmptyCells;
    }

    @Override
    public StreamingResponseBody exportToExcel(Long startDate, Long endDate, HttpServletResponse response) {

        LocalTime startTime = LocalTime.of(0, 0);
        LocalTime endTime = LocalTime.of(23, 59);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate == null || endDate == null) {
            throw new InvalidRequestException("both startDate and endDate request parameter is mandatory");
        }

        startDateTime = LocalDateTime.of(Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate(),
                startTime);
        endDateTime = LocalDateTime.of(Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate(),
                endTime);

        List<Transaction> transactions = transactionRepository.findByCreatedBetween(startDateTime, endDateTime);

        if (transactions.isEmpty()) {
            throw new InvalidRequestException("No data found in database");
        }

        return outputStream -> {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 SXSSFWorkbook workbook = new SXSSFWorkbook(SXSSFWorkbook.DEFAULT_WINDOW_SIZE)) {
                // Creating excel sheet
                String sheetName = "Transactions";
                Sheet sheet = workbook.createSheet(sheetName);

                // Creating font style for excel sheet
                Font headerFont = workbook.createFont();
                headerFont.setColor(IndexedColors.BLACK.getIndex());

                CellStyle headerColumnStyle = workbook.createCellStyle();
                headerColumnStyle.setFont(headerFont);

                // Row for header at 0 index
                Row headerRow = sheet.createRow(0);

                // Name of the columns to be added in the sheet
                String[] columns = new String[] { "id", "sender_id", "receiver_id", "initiator_id", "bank_code",
                        "service_code", "transaction_amount", "fee_amount", "status", "success", "refunded",
                        "created_date", "created_by", "modified_date", "modified_by" };

                // Creating header column at the first row
                for (int i = 0; i < columns.length; i++) {
                    Cell headerColumn = headerRow.createCell(i);
                    headerColumn.setCellValue(columns[i]);
                    headerColumn.setCellStyle(headerColumnStyle);
                }

                // Date formatting
                CellStyle dataColumnDateFormatStyle = workbook.createCellStyle();
                CreationHelper createHelper = workbook.getCreationHelper();
                dataColumnDateFormatStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yy h:mm;@"));

                // Adding data to sheet from the second row
                int rowIndex = 1;
                for (Transaction transaction : transactions) {
                    // Creating row for writing data
                    Row dataRow = sheet.createRow(rowIndex);

                    Cell columnId = dataRow.createCell(0);
                    columnId.setCellValue(transaction.getId() != null ? transaction.getId() : -1);

                    Cell columnSenderId = dataRow.createCell(1);
                    columnSenderId.setCellValue(transaction.getSenderId() != null ? transaction.getSenderId() : -1);

                    Cell columnReceiverId = dataRow.createCell(2);
                    columnReceiverId
                            .setCellValue(transaction.getReceiverId() != null ? transaction.getReceiverId() : -1);

                    Cell columnInitiatorId = dataRow.createCell(3);
                    columnInitiatorId
                            .setCellValue(transaction.getInitiatorId() != null ? transaction.getInitiatorId() : -1);

                    Cell columnBankCode = dataRow.createCell(4);
                    columnBankCode.setCellValue(transaction.getBankCode() != null ? transaction.getBankCode() : "");

                    Cell columnServiceCode = dataRow.createCell(5);
                    columnServiceCode.setCellValue(transaction.getServiceCode());

                    Cell columnTrxnAmount = dataRow.createCell(6);
                    columnTrxnAmount.setCellValue(transaction.getTrxnAmount());

                    Cell columnFeeAmount = dataRow.createCell(7);
                    columnFeeAmount.setCellValue(transaction.getFeeAmount());

                    Cell columnStatus = dataRow.createCell(8);
                    columnStatus.setCellValue(transaction.getStatus());

                    Cell columnIsSuccess = dataRow.createCell(9);
                    columnIsSuccess.setCellValue(transaction.isSuccess());

                    Cell columnIsRefunded = dataRow.createCell(10);
                    columnIsRefunded.setCellValue(transaction.isRefunded());

                    Cell columnCreated = dataRow.createCell(11);
                    columnCreated.setCellStyle(dataColumnDateFormatStyle);
                    columnCreated.setCellValue(transaction.getCreated() != null ? transaction.getCreated() : null);

                    Cell columnCreatedBy = dataRow.createCell(12);
                    columnCreatedBy.setCellValue(transaction.getCreatedBy() != null ? transaction.getCreatedBy() : -1);

                    Cell columnModified = dataRow.createCell(13);
                    columnModified.setCellStyle(dataColumnDateFormatStyle);
                    columnModified.setCellValue(transaction.getModified() != null ? transaction.getModified() : null);

                    Cell columnModifiedBy = dataRow.createCell(14);
                    columnModifiedBy
                            .setCellValue(transaction.getModifiedBy() != null ? transaction.getModifiedBy() : -1);

                    // Incrementing rowIndex by 1
                    rowIndex++;
                }

                workbook.write(out);
                workbook.dispose();

                String filename = "Transactions-" + startDate + "-" + endDate + ".xlsx";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                response.setContentLength((int) out.size());

                InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
                int BUFFER_SIZE = 1024;
                int bytesRead;
                byte[] buffer = new byte[BUFFER_SIZE];

                // Writing to output stream
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
        };
    }

}

