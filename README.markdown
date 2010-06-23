Athena
======

An efficient in-memory data structure supporting arbitrary boolean queries
--------------------------------------------------------------------------

Wiki, Mailing lists etc
-----------------------
These are hosted on [Google Code](http://code.google.com/p/athena-tag-store/)

License
-------
Athena is released under the GNU Lesser Public License V2.1. Please see
[http://www.gnu.org/licenses/lgpl-2.1.html](http://www.gnu.org/licenses/lgpl-2.1.html) for further information.

Overview
--------
Athena is an in-memory datastructure which allows you to store objects, associating each
with a set of Strings, known as tags.  You can then retrieve these objects by
specifying a boolean query on the tags.  The prototype implementation is written
in Java.

For example, imagine you stored three types of animal with these tags:

<pre>
["four-legs", "hair", "domesticated"] -> "dog"
["four-legs", "hair"] -> "wolf"
["two-legs", "feathers"] -> "bird"
</pre>

Imagine you wanted to retrieve the four legged animals, you could query for:

<pre>
  tag("four-legs")
</pre>

Or let's say you wanted to get all the non-domesticated four legged animals:

<pre>
  and(tag("four-legs"), not(tag("domesticated")))
</pre>

Athena allows you to do this, and what's more, it strives to allow you
to do this efficiently even with millions of objects and queries far more
complex than these simple examples.  It supports any combination of
"and", "or", and "not" boolean operations.

How it works
------------
So the naive approach to this problem would be to do an exhaustive scan of
all the objects you've got, checking each against the query, and returning
those that match.

And, in fact, the first time you query Athena, this is exactly what it does.
In other words, an exhaustive scan is the worst case scenario.  Fortunately,
Athena then starts to learn how to avoid this.

Currently objects may only be added to Athena, and once added, their tags
cannot be changed.  In the future this won't be the case, but we must
walk before we fly.

When objects are added, they are placed in an array (an ArrayList is currently
used for this).  Each object is accompanied by a map of "shortcuts", each of
which says "all of the objects after this one and before object X
do NOT match query Y".  This means that if you are looking for objects matching
query Y, then you can skip to object X.  It further means that if you are 
looking for objects that match query Z, and you know that no object that
matches query Z will match query Y, then you can also skip to object X.

So where do these shortcuts come from?  Well, much like Hansel and
Gretel dropped breadcrumbs, they are created while Athena is searching for 
stuff. Basically as Athena searches, it keeps track of objects it could have 
skipped, and creates shortcuts to avoid checking those objects in the future.
The process bears some resemblance to [skip lists](http://en.wikipedia.org/wiki/Skip_list).

Furthermore, since we can't have an infinite number of shortcuts, we only
keep those that seem to be useful (ie. we delete the least recently used 
shortcuts).

How to play with it
-------------------
It should work "out of the box" in [Eclipse](http://eclipse.org/).  If not
please [let me know](mailto:ian.clarke@gmail.com).

[This file](http://github.com/sanity/Athena/blob/master/src/athena/Test1.java)
provides a good example of basic usage.

Current status
--------------
Still just a prototype, doesn't support modification or deletion of objects,
and probably lots of room for efficiency improvements.  Also it probably
isn't thread safe.  All of these shortcomings should be addressable without
too much effort, this is still a very young endeavor.  I can't do this alone,
so if this excites you and you think you can help, please [let me know](mailto:ian.clarke@gmail.com)