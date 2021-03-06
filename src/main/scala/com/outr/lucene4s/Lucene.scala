package com.outr.lucene4s

import java.nio.file.Path

import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query._
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.index._
import org.apache.lucene.queries.mlt.MoreLikeThis

import scala.collection.JavaConverters._
import scala.language.experimental.macros

trait Lucene {
  private[lucene4s] var _fields = Set.empty[Field[_]]
  private[lucene4s] var _facets = Set.empty[FacetField]

  def directory: Option[Path]
  def defaultFullTextSearchable: Boolean
  def fields: Set[Field[_]] = _fields
  def field[T](name: String): Field[T] = {
    fields.find(_.name == name).getOrElse(throw new RuntimeException(s"Field $name not found")).asInstanceOf[Field[T]]
  }
  def facets: Set[FacetField] = _facets
  def facet(name: String): FacetField = {
    facets.find(_.name == name).getOrElse(throw new RuntimeException(s"Facet $name not found"))
  }
  def fullText: Field[String]
  def stopWords: Set[String]

  lazy val create: LuceneCreate = new LuceneCreate(this)

  def uniqueFields: List[String]
  def doc(): DocumentBuilder = new DocumentBuilder(this, None)
  def query(): QueryBuilder[SearchResult] = QueryBuilder(this, conversion = sr => sr)
  def delete(term: SearchTerm): Unit
  def deleteAll(): Unit
  def listen(listener: LuceneListener): Unit
  def commit(): Unit
  def update(searchTerm: SearchTerm): DocumentBuilder = new DocumentBuilder(this, Some(searchTerm))
  def dispose(): Unit

  protected[lucene4s] def analyzer: Analyzer
  protected[lucene4s] def facetsConfig: FacetsConfig
  protected[lucene4s] def taxonomyWriter: DirectoryTaxonomyWriter
  protected[lucene4s] def indexWriter: IndexWriter
  protected[lucene4s] def indexReader: IndexReader
  protected[lucene4s] def indexed(builder: DocumentBuilder): Unit
  protected[lucene4s] def withSearcherAndTaxonomy[R](f: SearcherAndTaxonomy => R): R

  protected[lucene4s] def moreLikeThis: MoreLikeThis = {
    val mlt = new MoreLikeThis(indexReader)
    mlt.setAnalyzer(analyzer)
    mlt.setStopWords(stopWords.asJava)

    mlt
  }
}

object Lucene {
  val specialCharacters = Set('~', '*', '?', '^', ':', '(', ')', '"', '-', '+', '\'')

  def isLuceneWord(word: String): Boolean = specialCharacters.exists(c => word.contains(c))
  def removeSpecialCharacters(text: String): String = text.filterNot(specialCharacters.contains)
  def queryToWords(query: String): List[String] = query.split(' ').toList.collect {
    case w if !w.equalsIgnoreCase("AND") && !w.equalsIgnoreCase("OR") => {
      val colon = w.indexOf(':')
      val term = if (colon > -1) {
        w.substring(colon + 1)
      } else {
        w
      }
      removeSpecialCharacters(term)
    }
  }
}