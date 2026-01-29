import { useMemo } from "react";
import { ApiReferenceReact } from "@scalar/api-reference-react";
import "@scalar/api-reference-react/style.css";
import yaml from "js-yaml";

// A thin wrapper around Scalar's ApiReference that adds sensible defaults
// while allowing full override via `configuration` prop.
export default function ScalarApiTester({
    spec,
    configuration,
}: {
    spec: string | object;
    configuration?: Record<string, any>;
}) {
    // Convert YAML/JSON string to object if needed
    const parsedSpec = useMemo(() => {
        if (typeof spec === "string") {
            try {
                // Check if spec is a valid URL
                if (spec.startsWith('https://') || spec.startsWith('http://')) {
                    return spec;
                }
                const obj = yaml.load(spec);
                if (obj && typeof obj === "object") return obj as object;
                console.error("Spec is not an object after parsing:", obj);
                return null;
            } catch (err) {
                console.error("Failed to parse spec (YAML/JSON):", err);
                return null;
            }
        } else if (spec && typeof spec === "object") {
            return spec;
        }
        return null;
    }, [spec]);

    // Title from spec if available (non-intrusive)
    const derivedTitle = useMemo(() => {
        try {
            return (parsedSpec as any)?.info?.title ?? undefined;
        } catch {
            return undefined;
        }
    }, [parsedSpec]);

    // Force remount of ApiReference when the spec changes so the UI updates
    const specKey = useMemo(() => {
        try {
            return typeof spec === "string" ? spec : JSON.stringify(spec);
        } catch {
            return String(Date.now());
        }
    }, [spec]);

    if (!parsedSpec) return <div>Invalid spec</div>;

    const baseConfig = {
        // Spec source
        title: derivedTitle,
        // Spec source
        ...(typeof parsedSpec === 'string'
                ? {url: parsedSpec}
                : {content: parsedSpec}
        ),

        // Networking
        proxyUrl: import.meta.env.VITE_SCALAR_PROXY_URL || "https://proxy.scalar.com",

        // Auth UX
        persistAuth: true,
        // authentication: {}, // left open for caller to provide

        // UI/UX defaults chosen to be safe and lightweight
        hideDownloadButton: false,
        hideTestRequestButton: false,
        orderRequiredPropertiesFirst: true,
        tagsSorter: "alpha" as const,
        operationsSorter: "method" as const,
        withDefaultFonts: false, // don't override app fonts/theme

        // Code samples
        defaultHttpClient: { targetKey: "js", clientKey: "fetch" },

        // Lifecycle hooks (no-ops by default)
        onServerChange: (_url: string) => {},
        onLoaded: () => {},
        onSpecUpdate: () => {},
    };

    // Allow full override via prop while keeping sensible defaults
    const mergedConfig = { ...baseConfig, ...(configuration || {}) };

    return (
        <div className="min-h-[400px]">
            <ApiReferenceReact key={specKey} configuration={mergedConfig} />
        </div>
    );
}
