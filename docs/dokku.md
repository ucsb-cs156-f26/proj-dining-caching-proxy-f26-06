
# Deploying on Dokku

To deploy on dokku, see the instructions here:

* <https://ucsb-cs156.github.io/topics/dokku/deploying_an_app.html>

You will also need the environment variables documented in
`docs/oauth.md` (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `ADMIN_EMAILS`)
and `docs/mongodb.md` (MongoDB setup).

You will also need the command:

* <tt>dokku git:set <i>appname</i> keep-git-dir true</tt>

## App names must match exactly

The prod and qa apps **must** be named `dining-caching-proxy` and
`dining-caching-proxy-qa` (with hyphens). The `48-tag-docker-image-main`
workflow derives these names from the repo name
(`proj-dining-caching-proxy-f26-NN` → `dining-caching-proxy` on `dokku-NN`)
and fails with `App dining-caching-proxy does not exist` if they are named
anything else (e.g. `diningcachingproxy`). The qa app also needs
`IS_TEMPLATE_APP=true`, since it is the template for PR review apps.

# Short Version

This short version omits many details, but if you are already familiar with the process of deploying applications, you may be able to use this.

Note that you may need to modify:
* `dining-caching-proxy` to `dining-caching-proxy-qa` or `dining-caching-proxy-dev-cgaucho` (where `cgaucho` is your github id)
* `https://github.com/ucsb-cs156/proj-dining-caching-proxy` to your team's repo url, if different
* `main` to `my-branch-name` for your feature branch
* `yourEmail@ucsb.edu` to your own email
* values for `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` from [docs/oauth.md](oauth.md)

### setup `dining-caching-proxy`

```
dokku apps:create dining-caching-proxy
dokku git:set dining-caching-proxy keep-git-dir true

dokku postgres:create dining-caching-proxy-db
dokku postgres:link dining-caching-proxy-db dining-caching-proxy --no-restart

dokku mongo:create dining-caching-proxy-m-db
dokku mongo:link dining-caching-proxy-m-db dining-caching-proxy --no-restart

dokku config:set --no-restart dining-caching-proxy PRODUCTION=true
dokku config:set --no-restart dining-caching-proxy SOURCE_REPO=https://github.com/ucsb-cs156/proj-dining-caching-proxy
dokku config:set --no-restart dining-caching-proxy GOOGLE_CLIENT_ID=get-value-from-google-developer-console
dokku config:set --no-restart dining-caching-proxy GOOGLE_CLIENT_SECRET=get-value-from-google-developer-console
dokku config:set --no-restart dining-caching-proxy ADMIN_EMAILS=list-of-admin-emails

dokku git:sync dining-caching-proxy https://github.com/ucsb-cs156/proj-dining-caching-proxy main
dokku ps:rebuild dining-caching-proxy

dokku letsencrypt:set dining-caching-proxy email yourEmail@ucsb.edu
dokku letsencrypt:enable dining-caching-proxy
```

### setup `dining-caching-proxy-qa`

```
dokku apps:create dining-caching-proxy-qa
dokku git:set dining-caching-proxy-qa keep-git-dir true

dokku postgres:create dining-caching-proxy-qa-db
dokku postgres:link dining-caching-proxy-qa-db dining-caching-proxy-qa --no-restart

dokku mongo:create dining-caching-proxy-qa-m-db
dokku mongo:link dining-caching-proxy-qa-m-db dining-caching-proxy-qa --no-restart

dokku config:set --no-restart dining-caching-proxy-qa PRODUCTION=true
dokku config:set --no-restart dining-caching-proxy-qa SOURCE_REPO=https://github.com/ucsb-cs156/proj-dining-caching-proxy
dokku config:set --no-restart dining-caching-proxy-qa GOOGLE_CLIENT_ID=get-value-from-google-developer-console
dokku config:set --no-restart dining-caching-proxy-qa GOOGLE_CLIENT_SECRET=get-value-from-google-developer-console
dokku config:set --no-restart dining-caching-proxy-qa ADMIN_EMAILS=list-of-admin-emails
dokku config:set --no-restart dining-caching-proxy-qa IS_TEMPLATE_APP=true

dokku git:sync dining-caching-proxy-qa https://github.com/ucsb-cs156/proj-dining-caching-proxy main
dokku ps:rebuild dining-caching-proxy-qa

dokku letsencrypt:set dining-caching-proxy-qa email yourEmail@ucsb.edu
dokku letsencrypt:enable dining-caching-proxy-qa
```
