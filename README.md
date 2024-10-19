# Site Dictation

Open-source language-learning for mass exposure to audio data spoken by humans.

Application for exercises of dictation (transcription of audio files spoken by native speakers for machine learning).
Uses spaced-repetition and pomodoro sessions.

Also intersperses translations from Google for deeper impressions of the native sentences.

## Getting Started

For local development, it's necessary to have files from https://commonvoice.mozilla.org/en/datasets
on your computer, in a directory specified as a Spring property or in a Spring profile.
The same with the location of the texts, from the transcripts are lifted.

If building with Maven locally, open a terminal in the target directory, and execute (for example)

    java -jar -Dspring.profiles.active=local -Ddictation.focus-language=german open-dictation-1.0.0.jar

To run the app from the command line (for example):

    mvn spring-boot:run -Dspring-boot.run.profiles=local -Ddictation.focus-language=romanian

Alternatively,

    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,thymeleaf -Ddictation.focus-language=german

But you also need to export certain environment variables to the shell.
Or if running the app in an IDE, such as IntelliJ, you need to add the variables to the "run/debug configuration".

| Var           |                            Example                             |                                                                                                                                                                                           Explanation |
|---------------|:--------------------------------------------------------------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| COMMON_VOICE_1_DE_BUCKET      |           /Users/you/Downloads/cv-corpus-de-1/clips/           |                                                                                                                                 Where common-voice version 1 German clips are located on your machine |
| COMMON_VOICE_17D_DE_BUCKET      | /Users/you/Downloads/cv-corpus-17.0-delta-2024-03-15/de/clips/ |                                                                                                                                               Path where is supplementary German mp3s on your machine |
| DICTATION_ADMIN_USER_NAME |                         foo@gmail.com                          |                                                                                                                       Username; used in case this application will be run in a multi-user environment |
| DICTATION_SENTENCES_BUCKET |              /Users/you/open-dictation/sentences/              |                                                                                                                                           Directory where are the corpora (TSV files) on your machine |
| DICTATION_TRANSACTIONS_BUCKET |            /Users/you/open-dictation/transactions/             |                                                                                                                Directory from which transactions are read and to which they are saved on your machine |
| GOOGLE_APPLICATION_CREDENTIALS | /Users/you/open-dictation/google/google-cloud-credentials.json | Directory where Google credentials for translation for your Google Cloud project are stored; required for translation feature; translation is free, at least at the rate of use of a solo user, AFAIK |

There are others of course, but you should not need them, unless you try to run this app with OAuth (for multiples locally, on a server, or in the cloud).
The Open AI credential should not be needed, unless image generation (DALLE) is enabled; but further work is needed on this feature.

## From where can I download the sentence files?

You can go to https://commonvoice.mozilla.org/en/datasets to download a dataset, from which, using `validated.tsv`,
you can generate a two-column TSV with the necessary data.

However, as I have a lot of work curating this data for my own use,
I will make some TSVs available in another repo: https://github.com/abushashia/open-dictation-data.
You could clone the repo locally or just download the files from GitHub as desired.

Among these, I've only published the files for Romanian (first language I learned with this app) and German (in progress).
Should there be enough demand for Spanish or French, for example, I could post files for those languages too.

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
