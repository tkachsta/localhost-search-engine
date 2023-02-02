# LocalHost Search Engine
The purpose of the project is to create powerfull application for developers as testing performance polygon for web parsing and writing its data to DataBase in highly concurrent 
environment. Every component (service) of solution can be changed separetly with a little influence on whole system. While developing this product I constatly kept in mind 
ways of how to increase performance of each operation. 

## Product Description
Product implements 3 layer architectire with includes Presentation layer, Business Logic layer, DataBase layer. Presentation layers responsible for how users will interact with application and stores all the views and logic of creating HTTP requests in the right way. The purpose of business logic is to handle these requests in most efficient way from perfromance, secturity, sclability stand point and relese HTTP responce as a desired answer for cosutomer interaction. DataBase layer carries responsibility to collect and store data for processing by business logic layer.
![image](https://user-images.githubusercontent.com/20218691/216282673-2b6f4187-8469-41d4-b7db-66b7af33d1a1.png)

### There are five services which are running concurrently when indexing starts:

**Parser** - service responsible for recursive pages parsing of web-sites, collecting required information and creating `PageEntit`y for each parsed page. Core elemnts are `ForkJoinPool` meachanism and `RecursiveAction`. Then result is pushed to `BlockingQueue` of Queue service for further processing by `LemmaFinder` serivce; <br><br>
**LemmaFinder** - this service finds and coollects lemma for each page from `List<PageEntity>` polled from `BlockingQueue` and uses key `LucenMorphology` library for that purposes. `LuceneMorphology` library is key Maven dependecy with currently supports russian words in application. Since all lemma found for defined `List<PageEntity>` then 
result of `LemmaFinder` service pushed to another `BlockingQueue` to process by `DataProcessing` service; <br> <br>
**DataProcessor** - service polls its task from `BlockingQueue` in order to create `List<LemmaEntity>`, `List<IndexIntity>` and bound with existing PageEntity for further processing by PageRepositoryQueue service. This one of the core service wich increases overall performance. This is because of creating LemmaEntity and IndexEntity by using JPA proxing object approach. Once data processer finished current task then is pushes task for PageRepositoryQueue service service and polls a new one to process; <br> <br>
**PageRepositoryQueue** - service polls its task from BlockingQueue and perform writing its data to database into `page` table. Once writing is done this service pushes task for LemmaRepositoryQueue service an polls a new one to process. In this service we can configure size of page batch to write. <br> <br>
**LemmaRepositoryQueue** - this is the last service in data flow pipline which grabs its task from another BlockingQueue with purpose to write its data into two tables `lemma` and `index`. <br> <br>
It's worth to mention that through described data pipline there are no a single reading from database. On each step of pipline we only writing data to database in order to sustain high performance by deacresing number of queries. In other words concept of logic is based on only writing without reading.

### Three is once service which is running when searching starts

### Threre is one injection servcie








## Architectire Description

## Implemented Technologies

## Algorithms and performance specifics 

## Steps to implement
