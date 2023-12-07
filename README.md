# RDF-star2Vec
[![License](https://img.shields.io/github/license/dwslab/jRDF2Vec)](https://github.com/dwslab/jRDF2Vec/blob/master/LICENSE)


RDF-star2Vec is a knowledge graph embedding model for RDF-star graphs. This repository is an extended implementation of <a href="https://github.com/dwslab/jRDF2Vec" target="_blank">jRDF2Vec</a>.
You can generate embeddings for `N-Triples` file.

## Walk strategies
RDF-star2Vec introduces graph walk methods that allow probabilistic transition between a quoted triple (QT) and its compositional entities.

- qs-walk (from QT to <i>subject</i>): walks from the QT to its compositional entity that is in the role of subject 
- oq-wal (from <i>object</i> to QT): walks from the compositional entity that is in the role of object to the QT.

## How to use

1. [Generate walk file](#generate-walk-file)
2. [Representation learning](#representation-learning)

### Generate walk file
ex) The source dataset is [rdf-star_ext_ikgrc2023.nt](https://github.com/aistairc/KGRC-RDF-star/blob/main/rdf-star_ext_ikgrc2023.nt), depth is `8`, probability of qs-walk is `0.5`, probability of oq-walk is `0.5`.

You can download the packaged JAR of the latest successful: [here](https://github.com/aistairc/RDF-star2Vec/releases)

```bash
java -jar rdf-star2vec_1.0.0-SNAPSHOT.jar -graph rdf-star_ext_ikgrc2023.nt -onlyWalks -walkDir experiment/ -walkGenerationMode STAR_MID_WALKS_DUPLICATE_FREE -depth 8 -qt2subject 0.5 -object2qt 0.5
```

#### Parameters for the Walk Configuration
- `-onlyWalks` (Required)
    - If added to the call, this switch will deactivate the training part so that only walks are generated. If training parameters are specified, they are ignored.
- `-qt2subject <number>` (Required, Range: 0 to 1)
    - The transition probability from a QT node to its compositional <i>subject</i>.
- `-object2qt <number>` (Required, Range: 0 to 1)
    - The transition probability from an <i>object</i> to a QT node.
- `-numberOfWalks <number>` (default: `100`)
    - The number of walks to be performed per entity.
- `-depth <depth>` (default: `4`)
    - This parameter controls the depth of each walk. Depth is defined as the number of hops. Hence, you can also set an odd number.
- `-walkGenerationMode <STAR_MID_WALKS | STAR_MID_WALKS_DUPLICATE_FREE | STAR_RANDOM_WALKS | STAR_RANDOM_WALKS_DUPLICATE_FREE>` (default: `STAR_MID_WALKS_DUPLICATE_FREE`)
    - This parameter determines the mode for the walk generation (multiple walk generation algorithms are available).
- `-threads <number_of_threads>` (default: (`# of available processors) / 2`)
    - This parameter allows you to set the number of threads that shall be used for the walk generation as well as for the training.
- `-walkDirectory <directory where walk files shall be generated/reside>`
    - The directory where the walks shall be generated into.


#### System Requirements
- Java 8 or later.

### Representation learning

The following steps are necessary to obtain RDF-star2Vec embeddings.

**Step 1: Compile wang2vec**<br/>
The RDF-star2Vec uses structured word2vec ([wang2vec](https://github.com/wlin12/wang2vec)).

Download the C implementation of [wang2vec from GitHub](https://github.com/wlin12/wang2vec).

Edit `word2vec.c` to support QT (long strings) in RDF-star as follows:
```bash
vi word2vec.c
```
```diff
#include <math.h>
#include <pthread.h>

- #define MAX_STRING 100
+ #define MAX_STRING 10000
#define MAX_EXP 6
#define MAX_SENTENCE_LENGTH 1000
```

Compile the files with `make`.

**Step 2: Run word2vec**<br/>
Run the compiled wang2vec implementation on the generated walk file using the [walk strategies](#walk-strategies). In case you receive a `segfault` error,
set the capping parameter to 1 (`-cap 1`).

*Call Syntax*<br/>
```bash
./word2vec -train <your walk file> -output <desired file to be written> - type <1 (skip-gram) or 3 (structured 
skip-gram>) -size <vector size> -threads <number of threads> -min-count 0 -cap 1  
```

*Exemplary Call*<br/>
```bash
./word2vec -train walks.txt -output v100.txt -type 3 -size 100 -threads 4 -min-count 0 -cap 1  
```

## Evaluation

Please see [here](https://github.com/aistairc/GEval-forKGRC-RDF-star) for evaluating the embeddings generated using RDF-star2Vec

## Publication
Egami, S., Ugai, T., Oota, M., Matsushita, K., Kawamura, T., Kozaki, K., Fukuda, K.: [RDF-star2Vec: RDF-star Graph Embeddings for Data Mining](./RDF-star2Vec%20-%20RDF-star%20Graph%20Embedings%20for%20Data%20Mining.pdf), IEEE Access, to appear