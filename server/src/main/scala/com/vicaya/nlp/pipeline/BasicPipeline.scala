package com.vicaya.nlp.pipeline

import edu.stanford.nlp.pipeline.{CoreDocument, StanfordCoreNLP}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object BasicPipeline {

    def apply(pipeline: StanfordCoreNLP, document: CoreDocument): BasicPipeline = {
      new BasicPipeline(pipeline, document)
    }
}

class BasicPipeline(pipeline: StanfordCoreNLP, document: CoreDocument) {

  def run(): Boolean = {
      Try(pipeline.annotate(document)) match {
        case Success(value) =>
          true
        case Failure(exception) =>
          false
      }
  }

  def nameEntity(): Unit = {
    document.sentences().asScala.map(sentence => {
      sentence.posTags().asScala.foreach(println)
    })
  }
}
