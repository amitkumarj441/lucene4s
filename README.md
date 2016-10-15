# lucene4s

[![Build Status](https://travis-ci.org/outr/lucene4s.svg?branch=master)](https://travis-ci.org/outr/lucene4s)
[![Stories in Ready](https://badge.waffle.io/outr/lucene4s.png?label=ready&title=Ready)](https://waffle.io/outr/lucene4s)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/outr/lucene4s)
[![Maven Central](https://img.shields.io/maven-central/v/com.outr/lucene4s_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/com.outr/lucene4s_2.11)

Light-weight convenience wrapper around Lucene to simplify complex tasks and add Scala sugar.

## Setup

lucene4s is published to Sonatype OSS and Maven Central currently supporting Scala 2.11.

Configuring the dependency in SBT simply requires:

```
libraryDependencies += "com.outr" %% "lucene4s" % "1.0.0"
```

You can also check-out the latest SNAPSHOT:

```
libraryDependencies += "com.outr" %% "lucene4s" % "1.1.0-SNAPSHOT"
```

Make sure you have the Sonatype Snapshots Resolver configured in SBT:

```
Resolver.sonatypeRepo("releases")
```

## Using

### Imports

You may find yourself needing other imports depending on what you're doing, but the majority of functionality can be
achieved simply importing `com.outr.lucene4s._`:

```scala
import com.outr.lucene4s._
```

### Creating a Lucene Instance

`Lucene` is the object utilized for doing anything with Lucene, so you first need to instantiate it:

```scala
val directory = Paths.get("index")
val lucene = new Lucene(directory = Option(directory))
```

NOTE: If you leave `directory` blank or set it to None (the default) it will use an in-memory index. 

### Creating Fields

For type-safety and convenience we can create the fields we'll be using in the document ahead of time:

```scala
val name = lucene.create.field[String]("name")
val address = lucene.create.field[String]("address")
```

### Inserting Documents

Inserting is quite easy using the document builder:

```scala
lucene.doc().fields(name("John Doe"), address("123 Somewhere Rd.")).index()
lucene.doc().fields(name("Jane Doe"), address("123 Somewhere Rd.")).index()
```

### Querying Documents

Querying documents is just as easy with the query builder:

```scala
val paged = lucene.query().sort(Sort(name)).search()
paged.results.foreach { searchResult =>
  println(s"Name: ${searchResult(name)}, Address: ${searchResult(address)}")
}
```

This will return a `PagedResults` instance with the page size set to the `limit`. There are convenience methods for
navigating the pagination and accessing the results.

The above code will output:

```
Name: John Doe, Address: 123 Somewhere Rd.
Name: Jane Doe, Address: 123 Somewhere Rd.
```

### Faceted Searching

See https://github.com/outr/lucene4s/blob/master/src/test/scala/tests/FacetsSpec.scala

### Full-Text Searching

See https://github.com/outr/lucene4s/blob/master/src/test/scala/tests/FullTextSpec.scala

### Case Class Support

lucene4s provides a powerful Macro-based system to generate two-way mappings between case classes and Lucene fields at
compile-time. This is accomplished through the use of `Searchable`.  The setup is pretty simple.

#### Setup

First we need to define a case class to model the data in the index:

```scala
case class Person(id: Int, firstName: String, lastName: String, age: Int, address: String, city: String, state: String, zip: String)
```

As you can see, this is a bare-bones case class with nothing special about it.

Next we need to define a `Searchable` trait the defines the unique identification for update and delete:

```scala
trait SearchablePerson extends Searchable[Person] {
  // This is necessary for update and delete to reference the correct document.
  override def idSearchTerm(person: Person): SearchTerm = exact(id(person.id))
  
  /*
    Though at compile-time all fields will be generated from the params in `Person`, for code-completion we can define
    an unimplemented method in order to properly reference the field. This will still compile without this definition,
    but most IDEs will complain.
   */
  def id: Field[Int]
}
```

As the last part of our set up we simply need to generate it from our `Lucene` instance:

```scala
val people = lucene.create.searchable[SearchablePerson]
```

#### Inserting

Now that we've configured everything inserting a person is trivial:

```scala
people.insert(Person(1, "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345")).index()
```

Notice that we still have to call `index()` at the end for it to actually invoke. This allows us to do more advanced
tasks like adding facets, adding non-Searchable fields, etc. before actually inserting.

#### Updating

Now lets try updating our `Person`:

```scala
people.update(Person(1, "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345")).index()
```

As you can see here, the signature is quite similar to `insert`. Internally this will utilize `idSearchTerm` as we
declared previously to apply the update. In this case that means as long as we don't change the id (1) then calls to
update will replace an existing record if one exists.

#### Querying

Querying works very much the same as in the previous examples, except we get our `QueryBuilder` from our `people`
instance:

```scala
val paged = people.query().search()
paged.entries.foreach { person =>
  println(s"Person: $person")
}
```

Note that instead of calling `paged.results` we call `paged.entries` as it represents the conversion to `Person`. We can
still use `paged.results` if we want access to the `SearchResult` like before.

#### Deleting

Deleting is just as easy as inserting and updating:

```scala
people.delete(Person(1, "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345"))
```

#### Additional Information

All `Searchable` implementations automatically define a `docType` field that is used to uniquely separate different
`Searchable` instances so you don't have to worry about multiple different instances overlapping.

For more examples see https://github.com/outr/lucene4s/blob/master/src/test/scala/tests/SearchableSpec.scala

## Versions

### Features for 1.1.0 (In-Progress)

* [X] Better field integrations and convenience implicits
* [X] Support for storing and retrieving case classes as documents via compile-time Macro
* [X] Numeric storage and retrieval functionality (Boolean, Int, Long, and Double)
* [X] Facets support in Searchable
* [X] Support for docType on Searchable to provide multiple Searchable implementation in a single index
* [ ] Range inserting and querying
* [ ] Dates
* [ ] Geospatial features
* [ ] Asynchronous features via Akka Futures
* [ ] Complete ScalaDocing

### Features for 1.0.0 (Released 2016.10.13)

* [X] Simplified set up of readers, writers, facets, etc. all under `Lucene`
* [X] DSL for inserting documents in a type-safe way
* [X] DSL for querying documents in a type-safe way
* [X] Facet searching support
* [X] Full-Text search functionality
* [X] Query Builder to simplify querying against Lucene
* [X] Solid test coverage
* [X] Updating and Deleting documents
* [X] Structured query terms builder