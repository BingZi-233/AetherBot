package online.bingzi.aetherbot.repository;

import online.bingzi.aetherbot.entity.CaTransaction;
import online.bingzi.aetherbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaTransactionRepository extends JpaRepository<CaTransaction, UUID> {
    
    /**
     * 查询用户的CA交易记录
     * 
     * @param user 用户
     * @return CA交易记录列表
     */
    List<CaTransaction> findByUserOrderByCreateTimeDesc(User user);
} 