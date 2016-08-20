AdvertiserSync Task | Markable.us
=================================

This is an interview programming challenge done by Tony Gaetani for Markable.us in August 2016.

The Challenge
-------------

Scheduled task runs a sequence of "Advertiser Sync" tasks, one by one. Each sync task must perform the following:

List products from the advertiser’s source (e.g., HTTP GET), which may be different for each advertiser. The sources
comes in various formats such as XML/JSON, with sometimes invalid/broken structure and encodings. For this task focus
on the XML case using provided sample data files below.

Upsert each product listed to the database, making these products as “active” (active: true). The format of source
product data will differ from advertiser to advertiser.

Reconcile the products listed with products previously listed for the advertiser. Any historical listings not included
in the most recent listing should be marked as “inactive” (active: false) in the database.

Once all of those activities are complete, the sync task is considered to be finished, and the next sync task may start.
Assume that the serial execution of sync tasks is an intentional design requirement. Assume advertiser's source is XML
for now, but different Product structure may occur - max 2 level deep.

Ambiguity
---------

What happens when the source is unrecognized or broken?
This is the crux of the problem - there is no answer.

What is the purpose of active/not active?
It is a flag used to determine the latest products for an advertiser.

Is the database enviroment sharded?
No.

Are transactions a possibility?
Yes.

My Solution
-----------

My initial idea was purely a theoretical design. Once the interview started it became clear that the interviewer wanted
me to live code which I was not prepared for because I do not have a good development machine right now (just an old
Dell laptop running ubuntu). For this reason we decided that I should create this repository and do the problem
in my own time and submit it to Markable.

I have my initial full system design written down, and if those ideas and decisions are relevant I am more than happy
to provide them at request. This document details the implementation in this repository.

XML Parsing
-----------

After some googling, it appears there are a few different options available. A traditional DOM parser would work to
parse the valid XML documents, but it completely fails on a broken XML document. There is also an HTML library that
should work to parse broken XML, but it does not provide a streaming interface and the XML files can be gigabytes in
size. Using a SAX parser makes sense because it can read an XML document up until the point of a failure, and it can
operate efficiently when parsing huge XML documents because it doesn't load the whole document into memory at once.

What can we do about broken XML data? My initial instict is to just not use it. In my mind, broken data should be
considered unusable. However, since that's not really an option here, I decided that we can use data up until the
point where it becomes broken and then throw the rest away. When the software encounters this situation, a message
should be logged describing what happened and what the data source was, and ideally there would be some monitoring
software that would notify someone who manages advertiser relations and they can tell the advertiser there is a problem.

I implemented a parser callback that looks for element starts and ends, as well as the text data. The callback will keep
track of what element start it saw last, and when it finds some text it writes the text into a map as the value and the
key is the element name. When we finally see an end product tag then we are ready to upsert/update. Then I find all
previous products in the database that have the same upc as this product are marked as active: false. Finally, the new
product is upserted into the database. This whole process is done as a transaction such that if marking the old products
as active: false succeeds but the upsert for new product details is failed, then the whole transaction is aborted and
the previous product is still active (not implemented).

Conclusion
----------

I did not finish implementing the solution because after about 30 minutes of digging through mongo documentation and
fighting with the driver (which I have never used before) I decided to just stop because this exercise is supposed to
take 45 minutes, and I spent at least 1.5 hours on it before stopping.