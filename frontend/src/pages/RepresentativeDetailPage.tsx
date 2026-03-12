import { useParams, Link } from 'react-router-dom'
import { Card, Stack, Text, Badge, Button, Group, Select, Textarea, Loader, Avatar } from '@mantine/core'
import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'

type Representative = {
  id: number
  name: string
  chamber: string
  state: string
  district?: string | null
  party?: string | null
  officialUrl?: string | null
}

type DonorSummary = {
  id: number
  representativeId: number
  cycleYear?: number | null
  source?: string | null
  totalAmount?: number | null
  topIndustry1?: string | null
  topIndustry2?: string | null
}

type RecentBill = {
  url: string
  title: string
  description: string
}

export default function RepresentativeDetailPage() {
  const { id } = useParams<{ id: string }>()
  const repId = id ? Number(id) : null
  const { token } = useAuth()
  const [note, setNote] = useState('')
  const [stance, setStance] = useState<string | null>(null)
  const [savedStance, setSavedStance] = useState<string | null>(null)
  const [savedNote, setSavedNote] = useState<string | null>(null)
  const [rep, setRep] = useState<Representative | null>(null)
  const [donor, setDonor] = useState<DonorSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [recentBills, setRecentBills] = useState<RecentBill[]>([])
  const [watchlistIds, setWatchlistIds] = useState<string[]>([])
  const [savingStance, setSavingStance] = useState(false)
  const [togglingWatchlist, setTogglingWatchlist] = useState(false)

  function authHeaders(extra?: HeadersInit): HeadersInit {
    const headers: HeadersInit = { ...(extra || {}) }
    if (token) headers['Authorization'] = `Bearer ${token}`
    return headers
  }

  useEffect(() => {
    if (!repId) return
    async function load() {
      try {
        setLoading(true)
        const [repRes, donorRes, watchlistRes, billsRes, stanceRes] = await Promise.all([
          fetch(`/api/representatives/${repId}`),
          fetch(`/api/donor-summaries?representativeId=${repId}`, { headers: authHeaders() }),
          fetch('/api/watchlist', { headers: authHeaders() }),
          fetch(`/api/representatives/${repId}/recent-bills`),
          fetch(`/api/stances/${repId}`, { headers: authHeaders() }),
        ])
        if (repRes.ok) {
          const repBody: Representative = await repRes.json()
          setRep(repBody)
        } else {
          setRep(null)
        }
        if (donorRes.ok) {
          const donorBody: DonorSummary = await donorRes.json()
          setDonor(donorBody)
        } else {
          setDonor(null)
        }
        if (watchlistRes.ok) {
          const watchlist: Representative[] = await watchlistRes.json()
          setWatchlistIds(watchlist.map(r => String(r.id)))
        } else {
          setWatchlistIds([])
        }
        if (billsRes.ok) {
          const bills: RecentBill[] = await billsRes.json()
          setRecentBills(bills.slice(0, 3))
        } else {
          setRecentBills([])
        }
        if (stanceRes.ok) {
          const body: { representativeId: number; stance: string; note?: string | null } = await stanceRes.json()
          setSavedStance(body.stance)
          setSavedNote(body.note ?? null)
        } else {
          setSavedStance(null)
          setSavedNote(null)
        }
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [repId])

  const onWatchlist = repId != null && watchlistIds.includes(String(repId))

  if (loading || !rep) {
    return loading ? <Loader /> : <Text>Representative not found.</Text>
  }

  async function handleWatchlist() {
    if (!repId) return
    try {
      setTogglingWatchlist(true)
      if (onWatchlist) {
        await fetch(`/api/watchlist/${repId}`, { method: 'DELETE', headers: authHeaders() })
        setWatchlistIds(ids => ids.filter(idStr => idStr !== String(repId)))
      } else {
        await fetch(`/api/watchlist/${repId}`, { method: 'POST', headers: authHeaders() })
        setWatchlistIds(ids => Array.from(new Set([...ids, String(repId)])))
      }
    } finally {
      setTogglingWatchlist(false)
    }
  }

  async function handleSetStance() {
    if (!repId || !stance) return
    try {
      setSavingStance(true)
      const params = new URLSearchParams()
      params.set('representativeId', String(repId))
      params.set('stance', stance)
      if (note) params.set('note', note)
      const res = await fetch(`/api/stances`, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
        body: params.toString(),
      })
      if (res.ok) {
        // Trust the stance and note we just sent; backend persistence is handled there.
        setSavedStance(stance)
        setSavedNote(note || null)
        setStance(null)
        setNote('')
      }
    } finally {
      setSavingStance(false)
    }
  }

  return (
    <Stack gap="md">
      <Button component={Link} to="/" variant="subtle" size="sm">← Back to dashboard</Button>
      <Card
        withBorder
        padding="lg"
        style={{
          borderColor:
            savedStance === 'SUPPORT'
              ? 'var(--mantine-color-green-6)'
              : savedStance === 'OPPOSE'
              ? 'var(--mantine-color-red-6)'
              : undefined,
          backgroundColor:
            savedStance === 'SUPPORT'
              ? 'var(--mantine-color-green-0)'
              : savedStance === 'OPPOSE'
              ? 'var(--mantine-color-red-0)'
              : undefined,
        }}
      >
        <Group justify="space-between">
          <div>
            <Text fw={700} size="xl">{rep.name}</Text>
            <Group gap="xs" mt="xs">
              <Badge>{rep.chamber}</Badge>
              <Badge variant="outline">{rep.state}</Badge>
              {rep.district && <Text size="sm">District {rep.district}</Text>}
              {rep.party && <Text size="sm" c="dimmed">{rep.party}</Text>}
            </Group>
            {savedNote ? (
              <Text mt="xs" size="sm" c="dimmed">{savedNote}</Text>
            ) : savedStance === 'SUPPORT' ? (
              <Text mt="xs" size="sm" c="green">I SUPPORT</Text>
            ) : savedStance === 'OPPOSE' ? (
              <Text mt="xs" size="sm" c="red">BOOOOOOOOO THIS PERSON SUCKS</Text>
            ) : savedStance === 'NEUTRAL' ? (
              <Text mt="xs" size="sm" c="dimmed">Neutral</Text>
            ) : null}
          </div>
          <Group gap="sm">
            <Avatar radius="xl" color="blue" size="lg">
              {rep.name.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase()}
            </Avatar>
            <Button variant={onWatchlist ? 'outline' : 'filled'} onClick={handleWatchlist}>
              {onWatchlist ? 'Remove from watchlist' : 'Add to watchlist'}
            </Button>
          </Group>
        </Group>
        {rep.officialUrl && (
          <Text mt="md" size="sm">
            <a href={rep.officialUrl} target="_blank" rel="noreferrer">Official link</a>
          </Text>
        )}
        <Text mt="xs" size="sm" c="dimmed">
          Bills and detailed voting history are available on{' '}
          <a
            href={`https://www.congress.gov/search?q=${encodeURIComponent(`{"source":"legislation","search":"${rep.name} ${rep.state}"}`)}`}
            target="_blank"
            rel="noreferrer"
          >
            Congress.gov
          </a>
          .
        </Text>
      </Card>

      {donor && (
        <Card withBorder padding="lg">
          <Text fw={600} mb="sm">Donor summary (mock)</Text>
          <Stack gap="xs">
            <Text size="sm">Source: {donor.source} {donor.cycleYear && `(${donor.cycleYear})`}</Text>
            {donor.totalAmount != null && <Text size="sm">Total: ${donor.totalAmount.toLocaleString()}</Text>}
            {(donor.topIndustry1 || donor.topIndustry2) && (
              <Text size="sm">Top industries: {[donor.topIndustry1, donor.topIndustry2].filter(Boolean).join(', ')}</Text>
            )}
          </Stack>
        </Card>
      )}

      {recentBills.length > 0 && (
        <Card withBorder padding="lg">
          <Text fw={600} mb="sm">Recent bills on Congress.gov</Text>
          <Stack gap="sm">
            {recentBills.map((bill) => (
              <Card
                key={bill.url}
                withBorder
                padding="md"
                component="a"
                href={bill.url}
                target="_blank"
                rel="noreferrer"
                style={{ textDecoration: 'none' }}
              >
                <Text fw={600} mb={4}>{bill.title}</Text>
                <Text size="sm" c="dimmed">{bill.description}</Text>
              </Card>
            ))}
          </Stack>
        </Card>
      )}

      <Card withBorder padding="lg">
        <Text fw={600} mb="sm">Your stance</Text>
        <Stack gap="sm">
          <Select
            placeholder="Support / Oppose / Neutral"
            data={[
              { value: 'SUPPORT', label: 'Support' },
              { value: 'OPPOSE', label: 'Oppose' },
              { value: 'NEUTRAL', label: 'Neutral' },
            ]}
            value={stance}
            onChange={setStance}
          />
          <Textarea placeholder="Note (optional)" value={note} onChange={(e) => setNote(e.target.value)} minRows={2} />
          <Button onClick={handleSetStance} disabled={!stance || savingStance}>Save stance</Button>
        </Stack>
      </Card>
    </Stack>
  )
}
