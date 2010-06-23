Athena, an efficient in-memory data structure supporting arbitrary boolean queries
==================================================================================

Overview
--------
Athena is a free Java library which allows you to store objects, associating each
with a set of Strings, known as tags.  You can then retrieve these objects by
specifying a boolean query on the tags.

For example, imagine you stored three types of animal with these tags:

["four-legs", "hair", "domesticated"] -> "dog"

["four-legs", "hair"] -> "wolf"

["two-legs", "feathers"] -> "bird"

Imagine you wanted to retrieve the four legged animals, you could query for:

  tag("four-legs")

Or let's say you wanted to get all the non-domesticated four legged animals:

  and(tag("four-legs"), not(tag("domesticated")))

