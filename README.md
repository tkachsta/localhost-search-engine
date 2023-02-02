# LocalHost Search Engine
The purpose of the project is to create powerfull application for developers as testing performance polygon for web parsing and writing its data to DataBase in highly concurrent 
environment. Every component (service) of solution can be changed separetly with a little influence on whole system. While developing this product I constatly kept in mind 
ways of how to increase performance of each operation. 

## Product Description
Product implements 3 layer architectire with includes Presentation layer, Business Logic layer, DataBase layer. Presentation layers responsible for how users will interact with application and stores all the views and logic of creating HTTP requests in the right way. The purpose of business logic is to handle these requests in most efficient way from perfromance, secturity, sclability stand point and relese HTTP responce as a desired answer for cosutomer interaction. DataBase layer carries responsibility to collect and store data for processing by business logic layer.
![image](https://user-images.githubusercontent.com/20218691/216282673-2b6f4187-8469-41d4-b7db-66b7af33d1a1.png)

There are five services which are running concurrently when indexing starts:<br>
**Parser** - service responsible for recursive pages parsing of web-sites, collecting required information and creating PageEntity for each parsed page. Core elemnts are ForkJoinPool meachanism and RecursiveAction. Then result is pushed to BlockingQueue of Queue service for further processing by LemmaFinder serivce; <br>
**LemmaFinder** - this service finds and coollects lemma for each page from List<PageEntity> polled from BlockingQueue and uses key LucenMorphology library for that purposes. LuceneMorphology library is key Maven dependecy with currently supports russian words in application. Since all lemma found for defined List<PageEntity> then 
result of LemmaFinder service pushed to another BlockingQueue to process by DataProcessing service; <br>
**DataProcessor** - service polls its task from BlockingQueue in order to create List<LemmaEntity>, List<IndexIntity> and bound with existing PageEntity for further processing by PageRepositoryQueue service. This one of the core service wich increases overall performance. This is because of creating LemmaEntity and IndexEntity by using JPA proxing object approach. Once data processer finished current task then is pushes task for PageRepositoryQueue service service and polls a new one to process; <br>




## Architectire Description

## Implemented Technologies

## Algorithms and performance specifics 

## Steps to implement
