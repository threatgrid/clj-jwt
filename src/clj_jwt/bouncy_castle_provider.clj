(ns clj-jwt.bouncy-castle-provider
  "Same trick used by pandect;
  see https://github.com/xsc/pandect/blob/main/src/pandect/utils/bouncy_castle_provider.clj"
  (:import java.security.Security
           org.bouncycastle.jce.provider.BouncyCastleProvider))

(defonce __install__
  (Security/addProvider (BouncyCastleProvider.)))
