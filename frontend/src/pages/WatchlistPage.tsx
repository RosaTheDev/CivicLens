import { useEffect, useState } from 'react'
import { Card, Stack, Text, Badge, Group, Loader, Avatar } from '@mantine/core'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

type Representative = {
  id: number
  name: string
  chamber: string
  state: string
  district?: string | null
  party?: string | null
  photoUrl?: string | null
}

type StanceSummary = {
  representativeId: number
  stance: string
  note?: string | null
}

export default function WatchlistPage() {
  const { token } = useAuth()
  const [watchlist, setWatchlist] = useState<Representative[]>([])
  const [stanceMap, setStanceMap] = useState<Record<number, string>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        setError(null)
        const headers: HeadersInit = {}
        if (token) headers['Authorization'] = `Bearer ${token}`
        const [watchRes, stanceRes] = await Promise.all([
          fetch('/api/watchlist', { headers }),
          fetch('/api/stances', { headers }),
        ])
        if (!watchRes.ok) {
          throw new Error(`Request failed with status ${watchRes.status}`)
        }
        const reps: Representative[] = await watchRes.json()
        setWatchlist(reps)

        if (stanceRes.ok) {
          const stances: StanceSummary[] = await stanceRes.json()
          const map: Record<number, string> = {}
          for (const s of stances) {
            map[s.representativeId] = s.stance
          }
          setStanceMap(map)
        } else {
          setStanceMap({})
        }
      } catch (e: any) {
        setError(e.message ?? 'Failed to load watchlist')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [token])

  if (loading) return <Loader />
  if (error) return <Text c="red">{error}</Text>

  return (
    <Stack gap="md">
      <div className="civic-hero">
        <Text fw={700} size="xl" className="civic-heading">My watchlist</Text>
        <Text size="sm" c="dimmed">Keep track of representatives you care about and your saved stances.</Text>
      </div>
      {watchlist.length === 0 ? (
        <Text c="dimmed">There are no representatives on your watch list :)</Text>
      ) : (
        <Stack gap="sm">
          {watchlist.map((rep) => {
            const stance = stanceMap[rep.id]
            return (
              <Card
                key={rep.id}
                withBorder
                padding="md"
                radius="md"
                shadow="xs"
                className="civic-glass-card civic-rep-card"
                component={Link}
                to={`/representatives/${rep.id}`}
                style={{
                  textDecoration: 'none',
                  color: 'inherit',
                  borderColor:
                    stance === 'SUPPORT'
                      ? 'var(--mantine-color-green-6)'
                      : stance === 'OPPOSE'
                      ? 'var(--mantine-color-red-6)'
                      : undefined,
                  backgroundColor:
                    stance === 'SUPPORT'
                      ? 'var(--mantine-color-green-0)'
                      : stance === 'OPPOSE'
                      ? 'var(--mantine-color-red-0)'
                      : undefined,
                }}
              >
                <Group justify="space-between" className="civic-list-card-body">
                  <Group gap="sm" align="flex-start">
                    <Avatar src={rep.photoUrl ?? undefined} radius="xl" color="blue">
                      {rep.name.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase()}
                    </Avatar>
                    <div>
                      <Text fw={600}>{rep.name}</Text>
                      <Group gap="xs" className="civic-list-meta">
                        <Badge size="sm" variant="light" className="civic-badge civic-badge--chamber">{rep.chamber}</Badge>
                        <Badge size="sm" variant="outline" className="civic-badge civic-badge--state">{rep.state}</Badge>
                        {rep.district && <Text size="sm" c="dimmed">District {rep.district}</Text>}
                        {rep.party && <Text size="sm" c="dimmed">{rep.party}</Text>}
                      </Group>
                      <div className="civic-list-divider" />
                      <Text size="sm" c="dimmed">
                        View profile to update stance notes and watchlist details.
                      </Text>
                    </div>
                  </Group>
                </Group>
              </Card>
            )
          })}
        </Stack>
      )}
    </Stack>
  )
}
