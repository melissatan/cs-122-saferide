# cs-122-saferide

## uchicago cs122 project (winter 2010) - saferide shuttle operator. 

(Putting this up mainly just in case my local disk ever crashes.)

This is based on the University of Chicago's Saferide system of point-to-point night shuttles, 
which was (and perhaps still is) coordinated manually by human operators.

The program does the same job. It simulates a shuttle coordinator who receives requests for shuttle pick-ups 
and drop-offs, and assigns each requester to the best-suited van operating at that time. 
It allows comparison of different van-passenger scheduling algorithms. 

Input: a map formatted in a certain way (map of Hyde Park included as demo). 

Shuttle requests are randomly generated. Best van is determined based on factors such as van location and van passengers. 

Output: map depicting each pick-up and drop-off, plus each van's movements. 

Findings (see Report.ppt): best algorithm varied depending on map size and unreachable nodes in the map 
(due to road blockages, dead-ends, etc). 
