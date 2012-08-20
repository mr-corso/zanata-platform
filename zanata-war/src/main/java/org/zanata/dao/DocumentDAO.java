package org.zanata.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.common.TransUnitCount;
import org.zanata.common.TransUnitWords;
import org.zanata.common.TranslationStats;
import org.zanata.model.HDocument;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;
import org.zanata.model.StatusCount;

@Name("documentDAO")
@AutoCreate
@Scope(ScopeType.STATELESS)
public class DocumentDAO extends AbstractDAOImpl<HDocument, Long>
{

   public DocumentDAO()
   {
      super(HDocument.class);
   }

   public DocumentDAO(Session session)
   {
      super(HDocument.class, session);
   }

   public HDocument getByDocIdAndIteration(HProjectIteration iteration, String id)
   {
      Criteria cr = getSession().createCriteria(HDocument.class);
      cr.add(Restrictions.naturalId().set("docId", id).set("projectIteration", iteration));
      cr.setCacheable(true).setComment("DocumentDAO.getByDocIdAndIteration");
      return (HDocument) cr.uniqueResult();
   }

   public HDocument getById(Long id)
   {
      Criteria cr = getSession().createCriteria(HDocument.class);
      cr.add(Restrictions.naturalId().set("id", id));
      cr.setCacheable(true).setComment("DocumentDAO.getById");
      return (HDocument) cr.uniqueResult();
   }

   public Set<LocaleId> getTargetLocales(HDocument hDoc)
   {
      // @formatter:off
      // TODO should this use UNIQUE?
      Query q = getSession().createQuery(
            "select tft.locale from HTextFlowTarget tft " +
            "where tft.textFlow.document = :document");
      q.setParameter("document", hDoc);
      @SuppressWarnings("unchecked")
      List<LocaleId> locales = q.list();
      // @formatter:on
      return new HashSet<LocaleId>(locales);
   }

   public int getTotalDocument()
   {
      Query q = getSession().createQuery("select count(*) from HDocument");
      q.setCacheable(true);
      Long totalCount = (Long) q.uniqueResult();
      if (totalCount == null)
         return 0;
      return totalCount.intValue();
   }

   public int getTotalActiveDocument()
   {
      Query q = getSession().createQuery("select count(*) from HDocument doc where doc.obsolete = false");
      q.setCacheable(true);
      Long totalCount = (Long) q.uniqueResult();
      if (totalCount == null)
         return 0;
      return totalCount.intValue();
   }

   public int getTotalObsoleteDocument()
   {
      Query q = getSession().createQuery("select count(*) from HDocument doc where doc.obsolete = true");
      q.setCacheable(true);
      Long totalCount = (Long) q.uniqueResult();
      if (totalCount == null)
         return 0;
      return totalCount.intValue();
   }

   /**
    * Returns the total message count for a document
    */
   public Long getTotalCountForDocument(HDocument document)
   {
      Session session = getSession();

      return
      (Long) session.createQuery(
            "select count(tf) from HTextFlow tf " +
                  "where tf.document = :doc and tf.obsolete = false")
            .setParameter("doc", document)
            .setCacheable(true).uniqueResult();
   }

   public Long getTotalWordCountForDocument(HDocument document)
   {
      Session session = getSession();

      return
      (Long) session.createQuery(
            "select sum(tf.wordCount) from HTextFlow tf " +
                  "where tf.document = :doc and tf.obsolete = false")
            .setParameter("doc", document)
            .setCacheable(true)
            .uniqueResult();
   }

   /**
    * @see ProjectIterationDAO#getStatisticsForContainer(Long, LocaleId)
    * @param docId
    * @param localeId
    * @return
    */
   public TranslationStats getStatistics(long docId, LocaleId localeId)
   {
      // @formatter:off
      Session session = getSession();

      // calculate unit counts
      @SuppressWarnings("unchecked")
      List<StatusCount> stats = session.createQuery(
         "select new org.zanata.model.StatusCount(tft.state, count(tft)) " + 
         "from HTextFlowTarget tft " + 
         "where tft.textFlow.document.id = :id " + 
         "  and tft.locale.localeId = :locale " + 
         "  and tft.textFlow.obsolete = false " + 
         "group by tft.state")
            .setParameter("id", docId)
            .setParameter("locale", localeId)
            .setCacheable(true).list();
      Long totalCount = getTotalCountForDocument( getById(docId) );

      TransUnitCount stat = new TransUnitCount();
      for (StatusCount count : stats)
      {
         stat.set(count.status, count.count.intValue());
      }
      int newCount = totalCount.intValue() - stat.get(ContentState.Approved) - stat.get(ContentState.NeedReview);
      stat.set(ContentState.New, newCount);

      // calculate word counts
      @SuppressWarnings("unchecked")
      List<StatusCount> wordStats = session.createQuery(
         "select new org.zanata.model.StatusCount(tft.state, sum(tft.textFlow.wordCount)) " + 
         "from HTextFlowTarget tft where tft.textFlow.document.id = :id " + 
         "  and tft.locale.localeId = :locale " + 
         "  and tft.textFlow.obsolete = false " + 
         "group by tft.state")
            .setParameter("id", docId)
            .setParameter("locale", localeId)
            .setCacheable(true)
            .list();
      Long totalWordCount = getTotalWordCountForDocument( getById(docId) );
      if (totalWordCount == null)
         totalWordCount = 0L;

      TransUnitWords wordCount = new TransUnitWords();
      for (StatusCount count : wordStats)
      {
         wordCount.set(count.status, count.count.intValue());
      }
      long newWordCount = totalWordCount.longValue() - wordCount.get(ContentState.Approved) - wordCount.get(ContentState.NeedReview);
      wordCount.set(ContentState.New, (int) newWordCount);

      TranslationStats transStats = new TranslationStats(stat, wordCount);
      return transStats;
      // @formatter:on
   }

   /**
    * Returns document statistics for multiple locales.
    *
    * @see DocumentDAO#getStatistics(long, org.zanata.common.LocaleId)
    * @param docId
    * @param localeIds If empty or null, data for all locales will be returned.
    * @return Map of document statistics indexed by locale. Some locales may not have entries if there is
    * no data stored for them.
    */
   public Map<LocaleId, TranslationStats> getStatistics(long docId, LocaleId ... localeIds)
   {
      // @formatter:off
      Session session = getSession();
      Map<LocaleId, TranslationStats> returnStats = new HashMap<LocaleId, TranslationStats>();
      Map<String, TransUnitCount> transUnitCountMap = new HashMap<String, TransUnitCount>();
      Map<String, TransUnitWords> transUnitWordsMap = new HashMap<String, TransUnitWords>();

      StringBuilder query = new StringBuilder();
      query.append("select new map (tft.state as state, count(tft) as count, ");
      query.append("          sum(tft.textFlow.wordCount) as wordCount, tft.locale.localeId as locale) ");
      query.append("from HTextFlowTarget tft ");
      query.append("where tft.textFlow.document.id = :id ");
      if( localeIds != null && localeIds.length > 0 )
      {
         query.append("  and tft.locale.localeId in (:locales) ");
      }
      query.append("  and tft.textFlow.obsolete = false ");
      query.append("group by tft.state, tft.locale");

      // calculate unit counts
      @SuppressWarnings("unchecked")
      Query hQuery = session.createQuery( query.toString() )
            .setParameter("id", docId)
            .setCacheable(true);

      if(localeIds != null && localeIds.length > 0)
      {
         hQuery.setParameterList("locales", localeIds);
      }

      List<Map> stats = hQuery.list();
      Map totalCounts = (Map) session.createQuery(
            "select new map ( count(tf) as count, sum(tf.wordCount) as wordCount ) " +
                  "from HTextFlow tf " +
                  "where tf.document.id = :id and tf.obsolete = false")
            .setParameter("id", docId)
            .setCacheable(true).uniqueResult();

      // Collect the results for all states
      for (Map row : stats)
      {
         ContentState state = (ContentState)row.get("state");
         Long count = (Long)row.get("count");
         Long wordCount = (Long) row.get("wordCount");
         LocaleId localeId = (LocaleId)row.get("locale");

         TransUnitCount transUnitCount = transUnitCountMap.get( localeId.getId() );
         if( transUnitCount == null )
         {
            transUnitCount = new TransUnitCount();
            transUnitCountMap.put(localeId.getId(), transUnitCount);
         }

         TransUnitWords transUnitWords = transUnitWordsMap.get( localeId.getId() );
         if( transUnitWords == null )
         {
            transUnitWords = new TransUnitWords();
            transUnitWordsMap.put(localeId.getId(), transUnitWords);
         }

         transUnitCount.set( state, count.intValue() );
         transUnitWords.set( state, wordCount.intValue() );
      }

      // Calculate the 'New' counts
      Long totalCount = (Long)totalCounts.get("count");
      Long totalWordCount = (Long)totalCounts.get("wordCount");
      for( TransUnitCount stat : transUnitCountMap.values() )
      {
         int newCount = totalCount.intValue() - stat.get(ContentState.Approved) - stat.get(ContentState.NeedReview);
         stat.set(ContentState.New, newCount);
      }
      for( TransUnitWords stat : transUnitWordsMap.values() )
      {
         int newCount = totalWordCount.intValue() - stat.get(ContentState.Approved) - stat.get(ContentState.NeedReview);
         stat.set(ContentState.New, newCount);
      }

      // Merge into a single Stats object
      for( String locale : transUnitCountMap.keySet() )
      {
         TranslationStats newStats = new TranslationStats(
               transUnitCountMap.get(locale), transUnitWordsMap.get(locale) );

         if( newStats.getUnitCount() != null && newStats.getWordCount() != null )
         {
            returnStats.put(new LocaleId(locale), newStats);
         }
      }

      return returnStats;
   }

   public void syncRevisions(HDocument doc, HTextFlow... textFlows)
   {
      int rev = doc.getRevision();
      syncRevisions(doc, rev, textFlows);
   }

   public void syncRevisions(HDocument doc, int revision, HTextFlow... textFlows)
   {
      doc.setRevision(revision);
      for (HTextFlow textFlow : textFlows)
      {
         textFlow.setRevision(revision);
      }
   }
   
   public HDocument getByProjectIterationAndDocId(final String projectSlug, final String iterationSlug, final String docId)
   {
      Session session = getSession();
      Query q = session.createQuery("from HDocument d where d.projectIteration.slug = :iterationSlug " +
            "and d.projectIteration.project.slug = :projectSlug " +
            "and d.docId = :docId " +
            "and d.obsolete = false");
      q.setParameter("iterationSlug", iterationSlug)
            .setParameter("projectSlug", projectSlug)
            .setParameter("docId", docId);
      q.setCacheable(true);
      final HDocument doc = (HDocument) q.uniqueResult();
      return doc;
   }
   
   public List<HDocument> getAllByProjectIteration(final String projectSlug, final String iterationSlug)
   {
      Session session = getSession();
      Query q = session.createQuery("from HDocument d " +
            "where d.projectIteration.slug = :iterationSlug " +
            "and d.projectIteration.project.slug = :projectSlug " +
            "and d.obsolete = false " +
            "order by d.name");
      q.setParameter("iterationSlug", iterationSlug)
            .setParameter("projectSlug", projectSlug);
      @SuppressWarnings("unchecked")
      final List<HDocument> documents = q.list();
      return documents;
   }

}
