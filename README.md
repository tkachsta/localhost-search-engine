# LocalHost Search Engine
The purpose of the project is to create powerfull application for developers as testing performance polygon for web parsing and writing its data to DataBase in highly concurrent environment. Every component (service) of solution can be changed separetly with a little influence on whole system. While developing this product I constatly kept in mind ways of how to increase performance of each operation.

## Product Description
From user stand point the product follows below value stream:
![image](https://user-images.githubusercontent.com/20218691/216328585-0f6b47d7-cce9-463a-a2de-5034b26add51.png)
- user chooses what websites to parse;
- application creates sets of lemma for every parsed web-page. Lemma - is normalized word in its basic form. <br> [More details about lematization](https://en.wikipedia.org/wiki/Lemmatisation)
- application creates indexes (unique objects) for every parsed page.
- application presents statistics of each parsed website;
- user recieves relevant search result to his query;

## HighLevel design Description
Product implements 3 layer architectire with includes Presentation layer, Business Logic layer, DataBase layer. Presentation layers responsible for how users will interact with application and stores all the views and logic of creating HTTP requests in the right way. The purpose of business logic is to handle these requests in most efficient way from perfromance, secturity, sclability stand point and relese HTTP responce as a desired answer for cosutomer interaction. DataBase layer carries responsibility to collect and store data for processing by business logic layer.
![image](https://user-images.githubusercontent.com/20218691/216282673-2b6f4187-8469-41d4-b7db-66b7af33d1a1.png)

### There are five services running concurrently when indexing starts:

**Parser** - service responsible for recursive pages parsing of web-sites, collecting required information and creating `PageEntit`y for each parsed page. Core elemnts are `ForkJoinPool` meachanism and `RecursiveAction`. Then result is pushed to `BlockingQueue` of Queue service for further processing by `LemmaFinder` serivce; <br><br>
**LemmaFinder** - this service finds and coollects lemma for each page from `List<PageEntity>` polled from `BlockingQueue` and uses key `LucenMorphology` library for that purposes. `LuceneMorphology` library is key Maven dependecy with currently supports russian words in application. Since all lemma found for defined `List<PageEntity>` then 
result of `LemmaFinder` service pushed to another `BlockingQueue` to process by `DataProcessing` service; <br> <br>
**DataProcessor** - service polls its task from `BlockingQueue` in order to create `List<LemmaEntity>`, `List<IndexIntity>` and bound with existing PageEntity for further processing by PageRepositoryQueue service. This one of the core service wich increases overall performance. This is because of creating LemmaEntity and IndexEntity by using JPA proxing object approach. Once data processer finished current task then is pushes task for PageRepositoryQueue service service and polls a new one to process; <br> <br>
**PageRepositoryQueue** - service polls its task from BlockingQueue and perform writing its data to database into `page` table. Once writing is done this service pushes task for LemmaRepositoryQueue service an polls a new one to process. In this service we can configure size of page batch to write. <br> <br>
**LemmaRepositoryQueue** - this is the last service in data flow pipline which grabs its task from another BlockingQueue with purpose to write its data into two tables `lemma` and `index`. <br> <br>
It's worth to mention that through described data pipline there are no a single reading from database. On each step of pipline we only writing data to database in order to sustain high performance by deacresing number of queries. In other words concept of logic is based on only writing without reading.

### There is one service running when searching starts
**SearchService** - this service is responsible for finding relevant responses to user's query. This is only service where logic mostly based on reading data from database. This service does following in context of searchpipline:
- Finds lemmas for user query (using instance of `LemmaFinder` service);
- Sets threshhold for lemma to consider in further processing;
- Performs sorting of lemma to start from most relevant one;
- Performs cleaning indexes to keep most relevante;
- Calculate relative and absolute relevanse;
- Find pages with highest relevance;
- Creates JSON objects to use as responce for API controller;

### There are two services running when statistics gathared
**StatisticsService** - this service gathers information during indexing such as number of indexed pages and lemmas, last time update, last occured error, status for each website. Also it provides overall statistics for all sites. Basic idea is to inject same statistic DTO object in above mentioned services to enrich with required data. So that the business logic doesnt spin around DTO instead DTO consumes data when it's required.<br> <br>
**Syncronization** - service's purpose is to sychronize statistics with existing data in database. 


## Key architectural approches 
Since speed and performance was critical that I wanted to make application with highest DataBase filling speed. In order to meet that purpose I created folowing principles that reflects in architecture:
- link web-page pre-screening increase speed greatly because we dont need to wait for timeout to expire. This is done simply by checking links ending so we only want .html ending or no ending at all;
- to parse pages I used Jsoup and while working `Connection.Response` object greatly increase overall speed;
- create `Document` after all validation is done and the to destoy it once we submitted task for another service. Don't spin logic around `Document` object;
- don't read from DataBase only write. All validation are spnining around parser service;
- use concurrent threads wihch are communicates through message queue of `Queue service`. In the picutre below you may say this principle visualized
![image](https://user-images.githubusercontent.com/20218691/216344291-6578ba13-f676-43e7-8915-66d26702763d.png)

## Implemented Technologies
- JAVA Collections
- JAVA Concurrancy
- JAVA JDBC
- JAVA Unit Testing
- JAVA OOP
- Hibernate
- Spring Data JPA
- SpringBoot Application
- Apache Lucene Morphology
- Jsoup
- MySQL

## Steps to implement
