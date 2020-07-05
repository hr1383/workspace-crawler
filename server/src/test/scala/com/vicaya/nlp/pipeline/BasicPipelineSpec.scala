package com.vicaya.nlp.pipeline

import java.util.Properties

import com.datasift.dropwizard.scala.test.BeforeAndAfterAllMulti
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.simple._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._
/*
CC Coordinating conjunction
CD Cardinal number
DT Determiner
EX Existential there
FW Foreign word
IN Preposition or subordinating conjunction
JJ Adjective
JJR Adjective, comparative
JJS Adjective, superlative
LS List item marker
MD Modal
NN Noun, singular or mass
NNS Noun, plural
NNP Proper noun, singular
NNPS Proper noun, plural
PDT Predeterminer
POS Possessive ending
PRP Personal pronoun
PRP$ Possessive pronoun
RB Adverb
RBR Adverb, comparative
RBS Adverb, superlative
RP Particle
SYM Symbol
TO to
UH Interjection
VB Verb, base form
VBD Verb, past tense
VBG Verb, gerund or present participle
VBN Verb, past participle
VBP Verb, non­3rd person singular present
VBZ Verb, 3rd person singular present
WDT Wh­determiner
WP Wh­pronoun
WP$ Possessive wh­pronoun
WRB Wh­adverb
 */
class BasicPipelineSpec extends FlatSpec with BeforeAndAfterAllMulti with Matchers {

  val properties: Properties = new Properties()
  properties.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,quote")
  properties.setProperty("coref.algorithm", "neural")

  //val pipeline: StanfordCoreNLP = new StanfordCoreNLP(properties)

  "Basepipeline" should "be able to parse simple text" in {
    val sampleText: String = "Find me all the pdf documents written by Mary"
    val document: Document = new Document(sampleText)

    document.sentences().asScala.foreach[Sentence]{ sent =>
      // We're only asking for words -- no need to load any models yet// We're only asking for words -- no need to load any models yet
      println(s"The second word of the sentence [$sent] is  ${sent.word(1)}")
      // When we ask for the lemma, it will load and run the part of speech tagger
      println(s"The third lemma of the sentence [$sent] is ${sent.lemma(2)}")
      // When we ask for the parse, it will load and run the parser
      println(s"The parse of the sentence [$sent] is ${sent.parse}")
      // Find all the NN and Person
      sent.nerTags.asScala.foreach(tag => {
        if (tag == "PERSON") {
          println(s"Identified a person $tag in [$sent]")
        } else {
          println(s"NOT Identified a person $tag in [$sent]")
        }
      })
      // Find all the NNP
      println(s"The mention the sentence [$sent] is ${sent.mentions()}}")
      val regex = "([{pos:/NN|NNS|NNP/}])" //Noun
      //val tokenSequencePattern: TokenSequencePattern = new TokenSequencePattern()
      println(s"Find sentiment -> ${sent.sentiment(properties).name()}")

      sent
    }

    //      val basicPipeline: BasicPipeline = new BasicPipeline(pipeline, document)
    //      basicPipeline.run()
    //
    //      // fetch information from pipeline
    //      println(s"${document.tokens()}")
  }

}
