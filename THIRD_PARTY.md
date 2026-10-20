# Third-party dependencies

The game's art and text are original. No assets or code from Ship Miner are included.

Downloaded dependencies are ignored by Git:

| Component | Version | License | Official project |
| --- | --- | --- | --- |
| Processing Core | 4.3.1 | LGPL-2.1 | https://processing.org/ |
| Eclipse Java compiler (ECJ) | 3.40.0 | EPL-2.0 | https://eclipse.dev/jdt/ |

The dependency jars contain their upstream license metadata. `tools/setup.sh` contains exact Maven Central artifact URLs and verified SHA-256 digests.

Optional cloud support uses Debian-packaged OpenJDK 21 (GPL-2.0 with Classpath exception) and Xvfb (X11/MIT-family licenses). Packages are downloaded through authenticated Debian repository metadata and extracted locally. Upstream Debian copyright files are retained under `.local/*/usr/share/doc/`.
