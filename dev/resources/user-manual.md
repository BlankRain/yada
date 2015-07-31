# The yada manual

Welcome to the yada manual!

This manual corresponds with version {{yada.version}}.

### Table of Contents

<toc drop="0"/>

### Audience

This manual is the authoritative documentation to yada. As such, it is
intended for a wide audience of developers, from beginners to experts.

### Get involved

If you are a more experienced Clojure or REST developer and would like
to get involved in influencing yada's future, please join our
[yada-discuss](https://groups.google.com/forum/#!forum/yada-discuss)
discussion group. Regardless of your experience, everyone is more than
welcome to join the list. List members will do their best to answer any
questions you might have.

### Spot an error?

If you spot a typo, misspelling, grammar problem, confusing text, or
anything you feel you could improve, please go-ahead and
[edit the source](https://github.com/juxt/yada/edit/master/dev/resources/user-manual.md). If
your contribution is accepted you will have our eternal gratitude, help
future readers and be forever acknowledged in the yada documentation as
a contributor!

[Back to index](/)

## Forward

State is everywhere. The world is moving and we need to keep up. We need our computers to do the same, keeping up-to-date with the latest information, trends, stock-prices, news and weather updates, and other important 'stuff'.

The web is primarily a means to move state around. You have some state
here, and you want it over there. Or it's over there, but you want it
over here.

For two decades or more, the pre-dominant model for web programming has ignored state, instead requiring developers to work at the level of the HTTP protocol itself.

For example, in Java...

```java
public void handleRequest(HttpServletRequest request,
                          HttpServletResponse response)
{
    response.setStatus(200);
}
```

or in Clojure

```clojure
(fn [request] {:status 200})
```

This programming model puts the HTTP request and response at centre
stage. The concept of state is missing entirely - the resource is seen
merely as an _operation_ (or set of operations) available for remote
invocation.

For years, the same RPC-centered approach has been copied by web
frameworks in most other languages, old and new (Python, Ruby, Go,
Clojure...). It has survived because it is so flexible, as most
low-level programming models are.

But there are significant downsides to this model too. HTTP is a big
specification, and it is unreasonable to expect developers to have the
time to implement all the relevant pieces of it. What's more, many
developers tend to implement much the same code over and over again, for
each and every 'operation' they write.

A notable variation on this programming model can be found in Erlang's
WebMachine and Clojure's Liberator. To a degree, these libraries ease
the burden on the developer by orchestrating the steps required to build
a response to a web request. However, developers are still required to
understand the state transition diagram underlying this orchestration if
they are to successfully exploit these libraries to the maximum
extent. Fundamentally, the programming model is the same: the developer
is still writing code with a view to forming a response at the protocol
level.

While this model has served as well in the past, there are increasingly
important reasons why we need an upgrade. Rather than mere playthings,
HTTP-based APIs are becoming critical components in virtually every
organisation. With supporting infrastructure such as proxies, API
gateways and monitoring, there has never been a greater need to improve
compatibility through better conformance with HTTP standards. Yet many
APIs today at best ignore, and worst violate many parts of the HTTP
standard.

It is time for a fresh approach. We need our libraries to do more work
for us. For this to happen, we need to move from the _de-facto_
'operational' view of web 'services' to a strong _data-oriented_
approach, focussing on what a web _resource_ is really about: _state_.

## Introduction

### What is yada?

yada is a Clojure library that lets you expose state to the web over
HTTP. But many libraries and 'web framworks' let you do that. What makes
yada different is the _programming model_ it offers developers, one that
is based on state rather than the HTTP protocol itself.

This approach has a number of advantages. Many things you would expect
to have to code yourself and taken care of automatically, leaving you
time to focus on other aspects of your application. And you end up with
far less networking code to write and maintain.

yada is built on a fully asynchronous foundation, allowing you to
exploit the asynchronous features of modern web servers, to achieve
greater scaleability for Clojure-powered your websites and APIs.

Above all, yada is data-centric, letting you specify your web resources
as data. This has some compelling advantages, such as being able to
transform that data into other formats, such as
[Swagger](http://swagger.io) specifications for API documentation.

### What yada is not

yada is not a fully-fledged web framework. It does not offer URI routing
and link formation, nor does it offer templating. It does, however,
integrate seamlessly with its sibling library
[bidi](https://github.com/juxt/bidi (for URI routing and formation) and
other routing libraries. It can integrated with the many template
libraries available for Clojure and Java, so you can build your own
web-framework from yada and other libraries.

## A tutorial: Hello World!

Let's introduce yada properly by writing some code. Let's start with some state, a string: `Hello World!`. We'll be able to give an overview of many of yada's features using this simple example.

We pass the string as a single argument to yada's `yada` function, and yada returns a web _resource_.

```clojure
(require '[yada.yada :as yada])

(yada/resource "Hello World!")
```

This web resource can be used as a Ring handler.

```clojure
(use 'aleph.http)

(start-server (yada/resource "Hello World!") {:port 3000})
```

Once we have bound this handler to the path `/hello`, we're able to make the following HTTP request :-

```nohighlight
curl -i http://{{prefix}}/hello
```

and receive a response like this :-

```http
HTTP/1.1 200 OK
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: {{now.date}}
Last-Modified: {{hello.date}}
Content-Type: text/plain;charset=utf-8
Vary: accept-charset
Content-Length: 13

Hello World!
```

Let's examine this response in detail.

The status code is `200 OK`. We didn't have to set it explicitly in
code, yada inferred the status from the request and the resource.

The first three response headers are added by our webserver, [Aleph](https://github.com/ztellman/aleph).

```http
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: {{now.date}}
```

(Note that currently, __Aleph is the only web server that yada supports__.)

Next we have another date.

```http
Last-Modified: {{hello.date}}
```

The __Last-Modified__ header shows when the string `Hello World!` was
created. As Java strings are immutable, yada is able to deduce that the
string's creation date is also the last time it could have been
modified.

Next we have a header telling us the media-type of the string's
state. yada is able to determine that the media-type is text, but
without more clues it must default to
`text/plain`.

It is, however, able to offer the body in numerous character set
encodings, thanks to the Java platform. The default character set of the
Java platform serving this resource is UTF-8, so yada inherits that as a
default.

```http
Content-Type: text/plain;charset=utf-8
```

Since the Java platform can encode a string in other charsets, yada uses the _Vary_ header to signal to the user-agent (and caches) that the body could change if a request contained an _Accept-Charset_ header.

```http
Vary: accept-charset
```

Next we are given the length of the body, in bytes.

```http
Content-Length: 13
```

In this case, the count includes a newline.

Finally we see our response body.

```nohighlight
Hello World!
```

### A conditional request

In HTTP, a conditional request is one where a user-agent (like a
browser) can ask a server for the state of the resource but only if a
particular condition holds. A common condition is whether the resource
has been modified since a particular date, usually because the
user-agent already has a copy of the resource's state which it can use
if possible. If the resource hasn't been modified since this date, the
server can tell the user-agent that there is no new version of the
state.

We can test this by setting the __If-Modified-Since__ header in the
request.

```nohighlight
curl -i {{prefix}}/hello -H "If-Modified-Since: {{hello.date}}"
```

The server responds with

```http
HTTP/1.1 304 Not Modified
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Tue, 21 Jul 2015 20:17:51 GMT
Content-Length: 0
```

### Mutation

Let's try to overwrite the string by using a `PUT`.

```nohighlight
curl -i {{prefix}}/hello -X PUT -d "Hello Dolly!"
```

The response is as follows (we'll omit the Aleph contributed headers from now on).

```http
HTTP/1.1 405 Method Not Allowed
Allow: GET, HEAD, OPTIONS
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: {{now.date}}
Content-Length: 0
```

The response status is `405 Method Not Allowed`, telling us that our
request was unacceptable. The is also a __Allow__ header, telling us
which methods are allowed. One of these methods is OPTIONS. Let's try this.

```nohighlight
curl -i {{prefix}}/hello -X OPTIONS
```

```http
HTTP/1.1 200 OK
Last-Modified: Thu, 23 Jul 2015 14:21:21 GMT
Allow: GET, HEAD, OPTIONS
Content-Type: text/plain;charset=utf-8
Vary: accept-charset
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Thu, 23 Jul 2015 14:22:12 GMT
Content-Length: 0
```

An `OPTIONS` response contains an __Allow__ header which tells us that `PUT` isn't possible.

We can't mutate our Java string, but we can put it into a Clojure
reference, swapping in different Java strings.

To demonstrate this, yada contains support for atoms (but you would usually employ a durable implementation).

```clojure
(yada/resource (atom "Hello World!"))
```

We can now make another `OPTIONS` request to see whether `PUT` is available.

```nohighlight
curl -i {{prefix}}/hello-atom -X OPTIONS
```

```http
HTTP/1.1 200 OK
Allow: GET, DELETE, HEAD, POST, OPTIONS, PUT
Vary:
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Thu, 23 Jul 2015 14:33:16 GMT
Content-Length: 0
```

It is! So let's try it.

```nohighlight
curl -i {{prefix}}/hello -X PUT -d "Hello Dolly!"
```

And now let's see if we've managed to change the state of the resource.

```http
HTTP/1.1 200 OK
Last-Modified: Thu, 23 Jul 2015 14:38:20 GMT
Content-Type: application/edn
Vary: accept-charset
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Thu, 23 Jul 2015 14:38:23 GMT
Content-Length: 14

Hello Dolly!
```

As long as someone else hasn't sneaked in a different state between your `PUT` and `GET`, and the server hasn't been restarted, you should see the new state of the resource is "Hello Dolly!".

### A HEAD request

There was one more method indicated by the __Allow__ header of our `OPTIONS` request, which was `HEAD`. Let's try this now.

```nohighlight
curl -i {{prefix}}/hello -X HEAD
```

```http
HTTP/1.1 200 OK
Last-Modified: Thu, 23 Jul 2015 14:41:20 GMT
Content-Type: text/plain;charset=utf-8
Vary: accept-charset
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Thu, 23 Jul 2015 14:42:26 GMT
Content-Length: 0
```

The response does not have a body, but tells us the headers we would get
if we were to try a `GET` request.

For more details about HREAD queries, see [insert reference here].

<!--
(Note that unlike Ring's implementation of `HEAD`
in `ring.middleware.head/wrap-head`, yada's implementation does not cause a
response body to be generated and then truncated. This means that HEAD requests in yada are fast and inexpensive.)
-->

### Parameters

Often, a resource's state will not be constant, but depend in some way on the request itself. Let's say we want to pass a parameter to the resource, via a query parameter.

First, let's call name our query parameter `p`. Since the state is
sensitive to the request, we specify a function rather than a value. The
function takes a single parameter called the _request context_, denoted
by the symbol `ctx`.

```clojure
(yada/resource
  (fn [ctx] (format "Hello %s!\n" (get-in ctx [:parameters :p])))
  :parameters {:get {:query {:p String}}})
```

Parameters are declared using additional key-value arguments after the first argument. They are declared on a per-method, per-type basis.

If the request correctly contains the parameter, it is available in the
request context, via the __:parameters__ key.

Let's see this in action

```nohighlight
curl -i {{prefix}}/hello-parameters?p=Ken
```

```http
HTTP/1.1 200 OK
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Mon, 27 Jul 2015 16:31:59 GMT
Content-Length: 7

Hi Ken
```

As well as query parameters, yada supports path parameters, request
headers, form parameters and whole request bodies. It can also coerce
parameters to a range of types. For more details, see
[insert reference here].

### Content negotiation

Content negotiation is an important feature of HTTP, allowing clients and servers to agree on how a resource can be represented to best meet the availability, compatibility and preferences of both parties.

For example, let's suppose we wanted to provide our greeting in multiple languages. We can specify a list of representations.

```clojure
(yada/resource
  (fn [ctx]
    (case (get-in ctx [:response :representation :language])
            "zh-ch" "你好世界!\n"
            "en" "Hello World!\n"))
  :representations [{:content-type "text/plain"
                     :language #{"en" "de" "zh-ch"}
                     :charset "UTF-8"}
                    {:content-type "text/plain"
                     :language "zh-CH"
                     :charset "Shift_JIS;q=0.9"}])
```

```http
HTTP/1.1 200 OK
Content-Type: text/plain;charset=utf-8
Vary: accept-language
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Mon, 27 Jul 2015 17:36:42 GMT
Content-Length: 14

你好世界!
```

```nohighlight
curl -i {{prefix}}/hello-languages -H "Accept-Language: zh-CH"
```


```nohighlight
curl -i http://localhost:8090/hello-languages -H "Accept-Charset: Shift_JIS" -H
"Accept: text/plain" -H "Accept-Language: zh-CH"
```

```http
HTTP/1.1 200 OK
Content-Type: text/plain;charset=shift_jis
Vary: accept-charset, accept-language, accept
Server: Aleph/0.4.0
Connection: Keep-Alive
Date: Mon, 27 Jul 2015 18:38:01 GMT
Content-Length: 9

?�D���E!
```

### An attempt to get the string gzip compressed

[todo]

### Swagger

[Swagger UI]({{prefix}}/swagger-ui/index.html?url=/hello-api/swagger.json)

## Async

[Hello World! in Chinese]

### Summary

This simple example demonstrated how a rich and functional HTTP resource
was created with a tiny amount of code. And yet, none of the behaviour
we have seen is hardcoded or contrived, everything was inferred from the
properties of the humble Java string, and yada includes support for many
other basic types (atoms, Clojure collections, URLs, files,
directories…).

But the real power of yada comes when you define your own resource
types, as we shall discover in subsequent chapters. But first, let's see
how to install and integrate yada in your web app.

## Installation

yada is a Clojure library and if you are using it directly from a
Clojure application with a Leiningen `project.clj` file, include the
following in the file's __:dependencies__ section.

```clojure
[yada "{{yada.version}}"]
```

If you want to use yada to create a web API, this is all you need to
do. But you can also clone the yada repository with `git`.

```nohighlight
git clone https://github.com/juxt/yada
```

You can then 'run' yada on your local machine to provide off-line access the documentation and demos.

```nohighlight
cd yada
lein run

```

(`lein` is available from [http://leiningen.org](http://leiningen.org))


## Resources - under the hood

Different types of resources are added to yada by defining types or records.

Let's delve a little deeper into how the _Hello World!_ example works.

Here is the actual code that tells yada about Java strings (comments removed).

```clojure
(defrecord StringResource [s last-modified]
  Resource
  (methods [this] #{:get :options})
  (parameters [_] nil)
  (exists? [this ctx] true)
  (last-modified [this _] last-modified)

  ResourceRepresentations
  (representations [_]
    [{:content-type #{"text/plain"}
      :charset platform-charsets}])

  Get
  (get* [this ctx] s))

(extend-protocol ResourceCoercion
  String
  (make-resource [s]
    (->StringResource s (to-date (now)))))
```

Recall the _Hello World!_ example.

```clojure
(yada/resource "Hello World!")
```

yada calls `make-resource` on the argument. This declaration causes a new instance of the `StringResource` record to be created.

```clojure
(extend-protocol ResourceCoercion
  String
  (make-resource [s]
  (->StringResource s (to-date (now)))))
```

The original string (`Hello World!`) and the current date is captured
and provided to the `StringResource` record.

The `StringResource` resource satisfies the `ResourceRepresentations`
protocol, which means it can specify which types of representation it is
able to generate. The `representations` function must return a list of
_representation declarations_, which declare all the possible
combinations of media-type, charset, encoding and language. In this
case, we just have one representation declaration which specifies
`text/plain` and the charsets available (all those supported on the Java
platform we are on).

### Extending yada

There are numerous types already built into yada, but you can also add
your own. You can also add your own custom methods.