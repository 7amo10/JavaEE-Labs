package com.ee.lab.perf.util;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

public class HibernateStatisticsInspector {

    public static Statistics getStatistics(EntityManager em) {
        Session session = em.unwrap(Session.class);
        Statistics stats = session.getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        return stats;
    }

    public static void reset(EntityManager em) {
        getStatistics(em).clear();
    }

    public static long getQueryCount(EntityManager em) {
        return getStatistics(em).getPrepareStatementCount();
    }
}
