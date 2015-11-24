# zen arcadia

A hackity webhook integration for github pull requests. In theory, it might support multiple actions, but as a spike it has just one action:
checking that commit summaries conform to a repo-specified regex.

## Checking commits

Add a file called `.zen-arcadia.yaml` to the root of your repo, with sample contents like:

```
checks:
  - check: commit
    params:
      - regex: '^\((maint|PUP-\d*)\).*$'
```

Note that the only field you'd want to change is the regex. The regex will be interpreted by Java (this being Clojure and all).
If you want to debug your regex, this could be a handy resource: http://www.regexplanet.com/advanced/java/index.html

## Running as a service

Create a github token, e.g. repos permissions for private repos if needed.

Expose this token in the environment variable: ZEN_ARCADIA_GITHUB_OAUTH_TOKEN

## Running Locally

Make sure you have Clojure installed.  Also, install the [Heroku Toolbelt](https://toolbelt.heroku.com/).

```sh
$ git clone https://github.com/kylog/zen-arcadia.git
$ cd zen-arcadia
$ lein repl
user=> (require 'zen-arcadia.web)
user=>(def server (zen-arcadia.web/-main))
```

Your app should now be running on [localhost:5000](http://localhost:5000/).

## Deploying to Heroku

```sh
$ heroku create
$ git push heroku master
$ heroku open
```
