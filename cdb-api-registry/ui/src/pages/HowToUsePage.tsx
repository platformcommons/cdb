import {useEffect, useMemo, useState} from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import {PanelLeftClose, PanelLeftOpen} from 'lucide-react'

// Static data loaded from code
import guides from '@docs/how-to-use/howto.json'



type Guide = {
    title: string
    detail: string
    contentRef: string // e.g., "getting-started.md"
}

// Import all markdown files in the md folder as raw strings
const mdModules = import.meta.glob('/src/docs/how-to-use/md/*.md', {as: 'raw', eager: true}) as Record<string, string>

function useGuideContent(filename?: string) {
    const [content, setContent] = useState<string>('')

    useEffect(() => {
        if (!filename) {
            setContent('');
            return
        }
        // Keys in mdModules are absolute like /src/docs/how-to-use/md/filename.md
        const key = `/src/docs/how-to-use/md/${filename}`
        const raw = mdModules[key]
        setContent(raw || `# Not found\nThe content file \`${filename}\` was not found.`)
    }, [filename])

    return content
}

export default function HowToUsePage() {
    const data = (guides as Guide[])
    const [query, setQuery] = useState('')
    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase()
        if (!q) return data
        return data.filter(g => g.title.toLowerCase().includes(q) || g.detail.toLowerCase().includes(q))
    }, [data, query])

    const [activeIndex, setActiveIndex] = useState(0)
    const active = filtered[activeIndex] ?? filtered[0] ?? data[0]
    const content = useGuideContent(active?.contentRef)

    // New: sidebar collapse state
    const [isSidebarCollapsed, setSidebarCollapsed] = useState(false)

    useEffect(() => {
        // Reset selection if filter changes
        setActiveIndex(0)
    }, [query])

    return (
        <div className="py-6">
            <div className="mb-4">
                <h1 className="text-2xl font-semibold text-gray-900">How to use</h1>
                <p className="mt-1 text-sm text-gray-600">Guides, best practices, and examples to get you productive
                    quickly.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left column: list & search */}
                {!isSidebarCollapsed && (
                    <div className="lg:col-span-1">
                        <div className="rounded-lg border bg-white p-4">
                            <div className="mb-3 flex items-center justify-between">
                                <span className="text-sm font-medium text-gray-700">Guides</span>
                                <button
                                    type="button"
                                    onClick={() => setSidebarCollapsed(true)}
                                    className="inline-flex items-center rounded-md border border-transparent bg-gray-100 px-2 py-1 text-gray-700 hover:bg-gray-200"
                                    aria-label="Collapse sidebar"
                                    title="Collapse sidebar"
                                >
                                    <PanelLeftClose className="h-4 w-4"/>
                                </button>
                            </div>

                            <div className="relative">
                                <input
                                    type="text"
                                    value={query}
                                    onChange={e => setQuery(e.target.value)}
                                    placeholder="Search guides..."
                                    className="w-full rounded-md border-gray-300 pr-10 focus:border-brand focus:ring-brand"
                                />
                                <svg className="absolute right-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400"
                                     viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                                    <path fillRule="evenodd"
                                          d="M12.9 14.32a8 8 0 111.414-1.414l3.387 3.387a1 1 0 01-1.414 1.414l-3.387-3.387zM14 8a6 6 0 11-12 0 6 6 0 0112 0z"
                                          clipRule="evenodd"/>
                                </svg>
                            </div>

                            <ul className="mt-4 space-y-1">
                                {filtered.map((g, i) => (
                                    <li key={g.contentRef}>
                                        <button
                                            onClick={() => {
                                                setActiveIndex(i);
                                                setSidebarCollapsed(true)
                                            }}
                                            className={`w-full text-left rounded-md px-3 py-2 transition-colors ${i === activeIndex ? 'bg-brand/10 text-brand' : 'hover:bg-gray-100'}`}
                                        >
                                            <div className="font-medium text-gray-900">{g.title}</div>
                                            <div className="text-sm text-gray-600 line-clamp-2">{g.detail}</div>
                                        </button>
                                    </li>
                                ))}
                                {filtered.length === 0 && (
                                    <li className="text-sm text-gray-500 px-3 py-2">No guides match your search.</li>
                                )}
                            </ul>
                        </div>
                    </div>
                )}

                {/* Right column: content */}
                <div className={isSidebarCollapsed ? 'lg:col-span-3' : 'lg:col-span-2'}>
                    <div className="rounded-lg border bg-white p-6">
                        {!!active && (
                            <>
                                <div className="mb-4 flex items-start justify-between">
                                    <div>
                                        <h2 className="text-xl font-semibold text-gray-900">{active.title}</h2>
                                        <p className="text-sm text-gray-600">{active.detail}</p>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        {isSidebarCollapsed && (
                                            <button
                                                type="button"
                                                onClick={() => setSidebarCollapsed(false)}
                                                className="inline-flex items-center rounded-md border border-transparent bg-gray-100 px-2 py-1 text-gray-700 hover:bg-gray-200"
                                                aria-label="Expand sidebar"
                                                title="Expand sidebar"
                                            >
                                                <PanelLeftOpen className="h-4 w-4"/>
                                            </button>
                                        )}
                                    </div>
                                </div>
                                <article className="prose prose-lg max-w-none prose-headings:scroll-mt-24 w-full">
                                    <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
                                </article>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
}
