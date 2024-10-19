# Site Dictation

Application for exercises of dictation (transcription of audio files spoken by native speakers for machine learning).
Uses spaced-repetition and pomodoro sessions.

Also intersperses translations from Google for deeper impressions of the native sentences.

## Getting Started

For local development, it's necessary to have files from https://commonvoice.mozilla.org/en/datasets
on your computer, in a directory specified as a Spring property or in a Spring profile.
The same with the location of the texts, from the transcripts are lifted.

If building with Maven locally, open a terminal in the target directory, and execute (for example)

    java -jar -Dspring.profiles.active=local -Ddictation.focus-language=romanian open-dictation-0.0.1-SNAPSHOT.jar

To run the app from the command line:

    mvn spring-boot:run -Dspring-boot.run.profiles=local -Ddictation.focus-language=romanian
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,thymeleaf -Ddictation.focus-language=german

But you also need to export certain environment variables to the shell... TODO

## Feature Requests

A google search into new tab for places, people, things with images, to reinforce typing them.
    Plus associate the picture with each, for memory, would be saved to s3 or locally
    Places, people, things-with-images would become metadata of each sentence

Reverse shadowing with AWS/Google

Speak-and-listen: listen to native, then see progress bar for same time, while you speak, just as fast.
    Consider also iframe for google translate. By Bryan's html is pretty good.
    Also, an audacity like visual would be good, for accents, pauses, and so on.

If the speakers have IDs in common-voice or other categorizations, need to remember these
for senatorial selection, rather than always blindly randomly selecting.
For example, 20 speakers, but 19 spoke only 10 sentences, 1 spoke thousands,
would pay to present the RARER speakers, for variety on this axis.

## How to enable HTTPS

You need a certificate and a domain, as you have with AWS.
When you setup your EB environment, add a classic load balancer (CLB).
You may edit the A type DNS record of `sitedictation.com` to route traffic to (CLB),
if you want to test it immediately.
However, you will migrate the CLB to an application load balancer (ALB), in the EC2 console.
Once this is done, edit the DNS A record again, to point to the new ALB.
At this point, the remaining step is to ensure traffic to `http://sitedictation.com`
is redirected to `https://sitedictation.com`.
Do this by editing the HTTP listener,
by changing its default action from a forwarding rule to a redirect to port 443,
leaving every other aspect of the URL the same.

Migrate from CLB to ALB, because for CLB, redirect rules cannot be configured directly in EC2.
You would have to add configuration to the source code of the EB application, to achieve this,
which, I think, talks to a proxy like nginx.

## Google Ads

Need image, use

    https://commons.wikimedia.org/wiki/File:Artificial_Intelligence_%26_AI_%26_Machine_Learning_-_30212411048.jpg
    https://pxhere.com/en/photo/1640118

Grammar through sound

    Prin urmare, am votat împotriva versiunii revizuit(e).

## Level 1

Levels call for separate positions per level and for cloze simplifications, a distinct template for "prepare" step,
which is preparation of submission of user transcript, to be diffed with official transcript.
Cloze levels call for separate text files for each level, so cloze n-gram stays fixed
Later levels before hardest allow venial mistakes. Hearts system, like in arcade games.
Also add setting to reserve positions with negative streak, use spaced repetition in this regard as well.
Hardest mode will still throw them at you again after 5 minutes, if selected.
Note, Fiat factor also determines level of difficult of a session.
All review in contrast is easier, but will not condition you for success and confidence.
New transcripts can also be introduced with cloze, so you could play in a mixed mode
    This could be distinct setting: easyNewPositions
In general, specific flags will be condensed into enumerated levels, as RebalancerInput.
New separate performance for each level too.
Then separate transactions and positions for each level as well.
