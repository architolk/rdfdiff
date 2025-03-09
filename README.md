# rdfdiff
Compares to RDF files and output any differences

Usage:

> `java -jar rdfdiff.jar <first.ttl> <second.ttl> <output> [-c]`

The first and second files will be compared. These files may be any RDF serialization format (turtle, XML/RDF, JSON-LD, N3, etc). Two output files will be created. The filename will start with the prefix give by <output>, for example "compare":

- `compare_1.ttl` will contain all triples present in first.ttl and not in second.ttl
- `compare_2.ttl` will contain all triples present in second.ttl and not in first.ttl

If the option `-c` is used, the output of all resources present in only of the files will be send to standard output (making it easier to spot differences in the URI minting between files).

See `rdfdiff.sh` for an example, using the data in de folder `data`.
