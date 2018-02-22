# The StationFinder was designed as a Workshop Tool for the Open Data Day at the UAS Technikum Vienna.
It makes use of the availability of realtime and station data through an API provided by the Viennese public transport (Wiener Linien).
Through a standard request via RESTful webservices and HTTP-GET the app recieves information concerning departure times, stop locations etc.

The difficulty in obtaining the information which is important to the user is the following:

when calling the API you need to pass one or more so-called "rbl" number. They represent an individual plattform of one line.
One station can be made up of one or more such rbl numbers, meaning if you want all the information for one particular station
you first have to find out the station id, with which you can gather all the belonging rbl numbers. These numbers can be found
in two different csv files, also provided by the Wiener Linien. 

So as you can see, the whole process is unfortunately not yet optimized, especially if you want to get the data through a search 
via station name. Implementing this process using a MYSQL Database, PHP and SQLite on Android like in this app aint rocket science, 
but it would be nice to have a more straightforward way of accessing the realtime data without having to query your db multiple times
before getting the neccessary information to even make the request. It feels like working your way around a problem that really should'nt
even exist in the first place.

The StationFinder has two modes for querying: the user can search by name of the station or by entering one or more rbl numbers.
He or she then sees an activity displaying a Google Map with markers showing the locations of the requested station plattforms with
labels that hold name, direction and departure times.
