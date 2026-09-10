(() => {
  const SELECTOR = "comment-item, reply-item";
  const LOCATION_MARKER = "data-comment-details-location";
  const RETRY_LIMIT = 8;
  const RETRY_DELAY = 100;
  const script = document.currentScript || document.getElementById("comment-details-script");
  const configuration = {
    locationFormat: script && script.dataset.locationFormat || "city",
    showUnknownLocation: script && script.dataset.showUnknownLocation === "true",
    showOnReplies: !script || script.dataset.showOnReplies !== "false"
  };

  const keys = {
    country: "comment-details.itsheep.com/location-country",
    region: "comment-details.itsheep.com/location-region",
    city: "comment-details.itsheep.com/location-city",
    unknown: "comment-details.itsheep.com/location-unknown"
  };

  const observedRoots = new WeakSet();
  const observer = new MutationObserver((records) => {
    for (const record of records) {
      for (const node of record.addedNodes) {
        inspectNode(node);
      }
    }
  });

  function inspectNode(node) {
    if (!(node instanceof Element)) {
      return;
    }
    inspectElement(node);
    node.querySelectorAll("*").forEach(inspectElement);
  }

  function inspectElement(element) {
    if (element.matches(SELECTOR)) {
      scheduleEnhancement(element);
    }
    if (element.shadowRoot && element.localName.includes("comment")) {
      observeRoot(element.shadowRoot);
    }
  }

  function scheduleEnhancement(host, attempt = 0) {
    window.setTimeout(() => enhance(host, attempt), attempt === 0 ? 0 : RETRY_DELAY);
  }

  function enhance(host, attempt) {
    if (!host.isConnected || host.localName === "reply-item" && !configuration.showOnReplies) {
      return;
    }

    const shadowRoot = host.shadowRoot;
    const details = host.comment || host.reply;
    if (!shadowRoot || !details) {
      retry(host, attempt);
      return;
    }

    observeRoot(shadowRoot);
    const baseComment = shadowRoot.querySelector("base-comment-item");
    if (!baseComment) {
      retry(host, attempt);
      return;
    }

    const location = formatLocation(details.metadata && details.metadata.annotations);
    if (!location) {
      return;
    }
    const baseShadowRoot = baseComment.shadowRoot;
    const time = baseShadowRoot && baseShadowRoot.querySelector("time.item-meta-info");
    if (!time) {
      retry(host, attempt);
      return;
    }
    if (baseShadowRoot.querySelector("[" + LOCATION_MARKER + "]")) {
      return;
    }

    observeRoot(baseShadowRoot);
    const element = document.createElement("span");
    element.className = time.className;
    element.setAttribute(LOCATION_MARKER, "");
    element.textContent = " • " + location;
    time.after(element);
  }

  function retry(host, attempt) {
    if (attempt < RETRY_LIMIT) {
      scheduleEnhancement(host, attempt + 1);
    }
  }

  function formatLocation(annotations) {
    if (!annotations) {
      return "";
    }
    const locationKeys = configuration.locationFormat === "country"
      ? [keys.country]
      : configuration.locationFormat === "region"
        ? [keys.region, keys.country]
        : [keys.city, keys.region, keys.country];
    for (const key of locationKeys) {
      if (annotations[key]) {
        return annotations[key];
      }
    }
    return configuration.showUnknownLocation && annotations[keys.unknown] === "true"
      ? "未知"
      : "";
  }


  function observeRoot(root) {
    if (observedRoots.has(root)) {
      return;
    }
    observedRoots.add(root);
    observer.observe(root, { childList: true, subtree: true });
    root.querySelectorAll("*").forEach(inspectElement);
  }

  function start() {
    observer.observe(document.documentElement, { childList: true, subtree: true });
    document.querySelectorAll("*").forEach(inspectElement);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start, { once: true });
  } else {
    start();
  }
})();
