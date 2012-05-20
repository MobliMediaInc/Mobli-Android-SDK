This open source Java library allows you to integrate Mobli into your Android application.The Mobli Android SDK is strongly derived from Facebook Android SDK, which is licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html)


What is the Mobli Android SDK?
===============
The developer’s Android SDK provides support for accessing and utilizing Mobli’s API. This access includes authentication via the OAuth 2.0 protocol and various REST requests.

The SDK is open source and is available on GitHub.


Getting Started
===============
See our [Getting Started Guide](http://developers.mobli.com/)


Installation
===============
Simply drag the "com" folder (inside the "src" folder) into your project's root directory.
Also, copy all the "close.png" icons into respective drawable folders.

Usage
===============
First of all, instantiate Mobli object:
Mobli mobli = new Mobli(YOUR_CLIENT_ID, YOUR_CLIENT_SECRET);

Now, you may do some of the following:

1) Retrieve public access_token:
AsyncMobliRunner runner = new AsyncMobliRunner(mobli);
runner.obtainPublicToken(…);

2) Prompt user to grant you the required permissions by authenticating to Mobli:
mobli.authorize(…);

3) Perform API call to one of Mobli's endpoints:
mobli.request(…);


Report Issues/Bugs
===============
[Bugs](mailto:devsupport@mobli.com)

[Questions](http://stackoverflow.com/questions/tagged/mobli)
