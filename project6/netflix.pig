NetflixData= LOAD '$G' USING PigStorage(',') AS (ID:chararray, rating:int, date:chararray);
FilteredData= FILTER NetflixData BY NOT (ID matches '.*:.*');
GroupedData= GROUP FilteredData by ID;
UserAvg = FOREACH GroupedData GENERATE FilteredData.ID, FLOOR(AVG(FilteredData.rating)* 10)/10 as avgRating;  
GroupedAvg= GROUP UserAvg BY avgRating;
Result = FOREACH GroupedAvg GENERATE group, COUNT(UserAvg);

STORE Result INTO '$O' USING PigStorage ('\t');
