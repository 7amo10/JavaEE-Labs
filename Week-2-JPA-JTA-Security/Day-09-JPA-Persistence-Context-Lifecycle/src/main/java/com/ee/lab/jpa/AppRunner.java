package com.ee.lab.jpa;

import com.ee.lab.jpa.entity.IncidentReport;
import com.ee.lab.jpa.entity.SeverityLevel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 2 DAY 09: JPA 3.1 PERSISTENCE CONTEXT & LIFECYCLE EVENTS");
        System.out.println("   Entity States | L1 Cache | Dirty Checking | flush/refresh | Callbacks");
        System.out.println("================================================================================");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JpaLifecyclePU");
        Long incidentId = null;

        try {
            // --------------------------------------------------------------------------------
            // SCENARIO 1: THE 4 ENTITY STATES (Transient -> Managed -> Detached -> Removed)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 1: THE 4 ENTITY STATES & MERGE SEMANTICS");
            System.out.println("--------------------------------------------------------------------------------");
            
            // 1. Transient State (New)
            IncidentReport report = new IncidentReport(
                "JVM Metaspace Exhaustion",
                "Metaspace reached 98% utilization on node-worker-01",
                SeverityLevel.CRITICAL
            );
            System.out.println(" [STATE: TRANSIENT] Incident created in memory. ID=" + report.getId());

            // 2. Transition to Managed State
            EntityManager em1 = emf.createEntityManager();
            em1.getTransaction().begin();
            em1.persist(report);
            System.out.println(" [STATE: MANAGED] em.persist() called. Persistent Context tracks entity: " + em1.contains(report));
            em1.getTransaction().commit();
            incidentId = report.getId();
            System.out.println(" [COMMITTED] Incident persisted with DB ID=" + incidentId + " (Rev=" + report.getRevision() + ")");
            em1.close();

            // 3. Transition to Detached State
            System.out.println(" [STATE: DETACHED] EntityManager closed. Entity is now detached: ID=" + report.getId());
            report.setIncidentTitle("MUTATED-WHILE-DETACHED: JVM Metaspace OOM"); // Mutated while detached

            // Verify detached mutations are NOT written without merge
            EntityManager emVerifyDetached = emf.createEntityManager();
            IncidentReport dbCheck = emVerifyDetached.find(IncidentReport.class, incidentId);
            System.out.println(" [DB CHECK DETACHED] Title in DB: '" + dbCheck.getIncidentTitle() + "' (Detached changes not saved)");
            emVerifyDetached.close();

            // 4. Merge Detached State back to Managed
            EntityManager em2 = emf.createEntityManager();
            em2.getTransaction().begin();
            IncidentReport mergedReport = em2.merge(report); // merge returns managed copy
            System.out.println(" [STATE: MERGED] em.merge() called. Original instance managed: " 
                + em2.contains(report) + " | Merged instance managed: " + em2.contains(mergedReport));
            em2.getTransaction().commit();
            em2.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 2: FIRST-LEVEL CACHE (IDENTITY MAP) & DIRTY CHECKING
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 2: L1 CACHE (IDENTITY MAP) & AUTOMATIC DIRTY CHECKING");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em3 = emf.createEntityManager();
            em3.getTransaction().begin();

            System.out.println(" [L1 CACHE TEST] Querying Incident ID=" + incidentId + " twice in same EntityManager...");
            IncidentReport ref1 = em3.find(IncidentReport.class, incidentId);
            IncidentReport ref2 = em3.find(IncidentReport.class, incidentId);

            boolean isSameReference = (ref1 == ref2);
            System.out.println(" [L1 IDENTITY MAP] ref1 == ref2 is: " + isSameReference + " (Zero SQL issued on second find!)");

            // Automatic Dirty Checking: Modify managed entity without calling persist() or merge()
            System.out.println(" [DIRTY CHECKING] Modifying managed entity field in memory: status -> 'RESOLVED'");
            ref1.resolve("Increased MaxMetaspaceSize to 512MB and recycled JVM");

            System.out.println(" [DIRTY CHECKING] Committing transaction (Hibernate issues automatic SQL UPDATE)...");
            em3.getTransaction().commit();
            em3.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 3: SYNCHRONIZATION MECHANICS: flush() vs refresh()
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 3: IN-FLIGHT SYNCHRONIZATION: flush() vs. refresh()");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em4 = emf.createEntityManager();
            em4.getTransaction().begin();

            IncidentReport reportToFlush = em4.find(IncidentReport.class, incidentId);
            reportToFlush.setIncidentTitle("FLUSHED-JVM-INCIDENT");
            
            System.out.println(" [EM FLUSH] Forcing SQL execution to DB before commit via em.flush()...");
            em4.flush(); // Forces SQL UPDATE immediately

            System.out.println(" [DIRTY IN-MEMORY] Altering title in memory to 'ACCIDENTAL-OVERWRITE'...");
            reportToFlush.setIncidentTitle("ACCIDENTAL-OVERWRITE");

            System.out.println(" [EM REFRESH] Discarding in-memory changes via em.refresh()...");
            em4.refresh(reportToFlush); // Overwrites memory with DB state

            System.out.println(" [REFRESH RESULT] Title restored from DB: '" + reportToFlush.getIncidentTitle() + "'");
            em4.getTransaction().commit();
            em4.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 4: ENTITY LIFECYCLE CALLBACKS & @Transient CALCULATION
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 4: @PostLoad & TRANSIENT DERIVED PROPERTY CALCULATION");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em5 = emf.createEntityManager();
            IncidentReport loadedReport = em5.find(IncidentReport.class, incidentId);
            System.out.println(" [POST-LOAD CHECK] Incident: '" + loadedReport.getIncidentTitle() + "'");
            System.out.println(" [POST-LOAD CHECK] Created At: " + loadedReport.getCreatedAt());
            System.out.println(" [POST-LOAD CHECK] Total Open Duration (Transient): " + loadedReport.getDurationOpenSeconds() + " seconds");
            System.out.println(" [POST-LOAD CHECK] Current Revision: " + loadedReport.getRevision());
            em5.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 5: ENTITY DELETION & REMOVED STATE TRANSITION
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 5: REMOVED STATE & DELETION LIFECYCLE");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em6 = emf.createEntityManager();
            em6.getTransaction().begin();

            IncidentReport reportToDelete = em6.find(IncidentReport.class, incidentId);
            System.out.println(" [STATE: REMOVED] Calling em.remove()...");
            em6.remove(reportToDelete);
            System.out.println(" [MANAGED CHECK] em.contains(reportToDelete) before commit: " + em6.contains(reportToDelete));
            
            em6.getTransaction().commit();
            em6.close();

            EntityManager emVerifyDelete = emf.createEntityManager();
            IncidentReport deletedCheck = emVerifyDelete.find(IncidentReport.class, incidentId);
            System.out.println(" [POST-DELETE VERIFICATION] Querying deleted ID=" + incidentId + " -> Found: " + (deletedCheck != null));
            emVerifyDelete.close();

        } finally {
            emf.close();
            System.out.println("\n================================================================================");
            System.out.println("               JPA LIFECYCLE LAB COMPLETED SUCCESSFULLY");
            System.out.println("================================================================================");
        }
    }
}
