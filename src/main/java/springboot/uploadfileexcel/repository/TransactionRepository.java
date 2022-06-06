package springboot.uploadfileexcel.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import springboot.uploadfileexcel.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    List<Transaction> findByCreatedBetween(LocalDateTime startDate, LocalDateTime endDate);
}