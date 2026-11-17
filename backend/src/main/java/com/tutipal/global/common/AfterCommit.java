package com.tutipal.global.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 트랜잭션 안이면 커밋된 뒤에, 아니면 바로 실행 (알림을 받은 화면이 다시 읽을 때 저장 전 값을 보지 않게). */
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
