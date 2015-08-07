# review-delays

A crappy little script I threw together to check for gender bias in
time-to-first-comment on GitHub pull requests.

## Installation

Install [leiningen](http://leiningen.org). Then, you can compile the jar via:

    $ lein uberjar

## Usage

The first thing you probably want to do is open up
`src/review\_delays/core.clj` and modify `munge-data` to accurately tag your
data. You may need to modify `graph-dataset` as well to play with data
representations and other stuff.

    $ lein run -- -u GITHUB_USERNAME -t GITHUB_API_TOKEN ORGANIZATION_NAME

## License

Copyright © 2015 Shanti Chellaram

Distributed under the MIT License
