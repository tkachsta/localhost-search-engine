# LocalHost Search Engine
The purpose of the project is to create powerful application for developers as testing performance polygon for web parsing and writing its data to DataBase in highly concurrent environment. Every component (service) of solution can be changed separately with a little influence on whole system. While developing this product I constantly kept in mind ways of how to increase performance of each operation.
<details><summary>Illustrating indexing process:</summary>
<p>
![Honeycam 2023-02-03 11-30-12](https://user-images.githubusercontent.com/20218691/216552025-6707aac7-f84b-4bed-9a79-1eff3f7d684b.gif)
</p>
</details>
Illustrating searching process:
![Honeycam 2023-02-03 11-35-36](https://user-images.githubusercontent.com/20218691/216552080-d6ea3d62-90cd-43c6-97d6-2085d89ca21a.gif)

## Product Description
From user standpoint the product follows below value stream:
![image](https://user-images.githubusercontent.com/20218691/216328585-0f6b47d7-cce9-463a-a2de-5034b26add51.png)
- user chooses what websites to parse;
- application creates sets of lemma for every parsed web-page. Lemma - is normalized word in its basic form. <br> [More details about lematization](https://en.wikipedia.org/wiki/Lemmatisation)
- application creates indexes (unique objects) for every parsed page.
- application presents statistics of each parsed website;
- user receives relevant search result to his query;

## HighLevel design Description
Product implements 3 layer architecture with includes Presentation layer, Business Logic layer, DataBase layer. Presentation layers responsible for how users will interact with application and stores all the views and logic of creating HTTP requests in the right way. The purpose of business logic is to handle these requests in most efficient way from performance, security, scalability standpoint and release HTTP response as a desired answer for costumer interaction. DataBase layer carries responsibility to collect and store data for processing by business logic layer.
![image](https://user-images.githubusercontent.com/20218691/216282673-2b6f4187-8469-41d4-b7db-66b7af33d1a1.png)

### There are five services running concurrently when indexing starts:

**Parser** - service responsible for recursive pages parsing of web-sites, collecting required information and creating `PageEntit`y for each parsed page. Core elements are `ForkJoinPool` mechanism and `RecursiveAction`. Then result is pushed to `BlockingQueue` of Queue service for further processing by `LemmaFinder` service; <br><br>
**LemmaFinder** - this service finds and collects lemma for each page from `List<PageEntity>` polled from `BlockingQueue` and uses key `LucenMorphology` library for that purposes. `LuceneMorphology` library is key Maven dependency with currently supports russian words in application. Since all lemma found for defined `List<PageEntity>` then 
result of `LemmaFinder` service pushed to another `BlockingQueue` to process by `DataProcessing` service; <br> <br>
**DataProcessor** - service polls its task from `BlockingQueue` in order to create `List<LemmaEntity>`, `List<IndexIntity>` and bound with existing PageEntity for further processing by PageRepositoryQueue service. This one of the core service which increases overall performance. This is because of creating LemmaEntity and IndexEntity by using JPA proxy object approach. Once data processor finished current task then is pushes task for PageRepositoryQueue service and polls a new one to process; <br> <br>
**PageRepositoryQueue** - service polls its task from BlockingQueue and perform writing its data to database into `page` table. Once writing is done this service pushes task for LemmaRepositoryQueue service and polls a new one to process. In this service we can configure size of page batch to write. <br> <br>
**LemmaRepositoryQueue** - this is the last service in data flow pipeline which grabs its task from another BlockingQueue with purpose to write its data into two tables `lemma` and `index`. <br> <br>
It's worth to mention that through described data pipeline there are no a single reading from database. On each step of pipeline we're only writing data to database in order to sustain high performance by decreasing number of queries. In other words concept of logic is based on only writing without reading.

### There is one service running when searching starts
**SearchService** - this service is responsible for finding relevant responses to user's query. This is only service where logic mostly based on reading data from database. This service does following in context of searchlight:
- Finds lemmas for user query (using instance of `LemmaFinder` service);
- Sets threshold for lemma to consider in further processing;
- Performs sorting of lemma to start from most relevant one;
- Performs cleaning indexes to keep most relevant;
- Calculate relative and absolute relevance;
- Find pages with the highest relevance;
- Creates JSON objects to use as response for API controller;

### There are two services running when statistics gathered
**StatisticsService** - this service gathers information during indexing such as number of indexed pages and lemmas, last time update, last occurred error, status for each website. Also, it provides overall statistics for all sites. Basic idea is to inject same statistic DTO object in above-mentioned services to enrich with required data. So that the business logic doesn't spin around DTO instead DTO consumes data when it's required.<br> <br>
**Synchronization** - service's purpose is to synchronize statistics with existing data in database. 


## Key architectural approaches 
Since speed and performance was critical that I wanted to make application with highest DataBase filling speed. In order to meet that purpose I created following principles that reflects in architecture:
- link web-page pre-screening increase speed greatly because we don't need to wait for timeout to expire. This is done simply by checking links ending, so we only want .html ending or no ending at all;
- to parse pages I used Jsoup and while working `Connection.Response` object greatly increase overall speed;
- create `Document` after all validation is done. Don't spin logic around `Document` object;
- don't read from DataBase only write. All validation are spinning around parser service;
- use concurrent threads which are communicates through message queue of `Queue service`. In the picture below you may say this principle visualized
![image](https://user-images.githubusercontent.com/20218691/216344291-6578ba13-f676-43e7-8915-66d26702763d.png)

## Implemented Technologies
- JAVA Collections
- JAVA Concurrency
- JAVA JDBC
- JAVA Unit Testing
- JAVA OOP
- Hibernate
- Spring Data JPA
- SpringBoot Application
- Apache Lucene Morphology
- REST API
- Jsoup
- Jackson
- MySQL
- Maven

## Steps to implement
1. Create new schema `search_engine` in MySQL WorkBench or similar tool;
2. In `application.yaml` file set up your connection details under section `spring.datasource`;
3. In `application.yaml` file set up websites under section `indexing_settings` you want to parse and index for searching
4. Start SpringBott Application from `Application` class;
5. In your web-browser enter `http://localhost:8080/`. That's it! Application ready to use on your local host.
