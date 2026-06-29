# quanta-asset [![Clojars Project](https://img.shields.io/clojars/v/io.github.clojure-quant/quanta-asset.svg)](https://clojars.org/io.github.clojure-quant/quanta-asset)

Instrument and asset database for the quanta ecosystem.

- `quanta.asset.schema` — Datahike schema for assets and symbol lists
- `quanta.asset.datahike` — asset DB queries and transacts (Datahike)
- `quanta.asset.db` — in-memory instrument registry
- `quanta.asset.futures` — futures symbol resolution

## DEMO

```
cd demo
clojure -X:print          # seed db and print assets + quote-lists
clojure -X:server         # start modular clip system (keeps db running)
```

Demo data: `demo-assets.edn` and `demo-lists/*.edn`.
